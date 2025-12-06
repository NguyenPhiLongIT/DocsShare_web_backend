import os
import re
import tempfile
import base64
import pickle
import torch

import numpy as np
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename

from toxic.service import predict_toxic

try:
    from toxic.service_vn import predict_texts
    VN_MODEL_AVAILABLE = True
except Exception as e:
    print("[warning] Could not load Vietnamese toxic model:", e)
    VN_MODEL_AVAILABLE = False
    predict_texts = None  # tránh NameError

from summary.service import summarize_text
from summary.extract import extract_n_sentences
from cbir.extract_img import extract_images_from_pdf
from cbir.export_features import load_features_from_db
from cbir.service import (
    load_model,
    transformations,
    get_latent_features,
    get_latent_features_img,
    perform_search,
)
from semantic.semantic_search import semantic_bp


app = Flask(__name__)
app.register_blueprint(semantic_bp)


MODEL_PATH = "cbir/conv_autoencoderv2_200ep_3.pt"
model = load_model(MODEL_PATH)


LABELS = ["toxic", "severe_toxic", "obscene", "threat", "insult", "identity_hate"]

THRESHOLDS = {
    "vi": {
        "toxic": 0.8,
        "severe_toxic": 0.8,
        "obscene": 0.8,
        "threat": 0.8,
        "insult": 0.8,
        "identity_hate": 0.8,
    },
    "en": {
        "toxic": 0.6,
        "severe_toxic": 0.3,
        "obscene": 0.6,
        "threat": 0.3,
        "insult": 0.6,
        "identity_hate": 0.3,
    },
}

_VI_REGEX = re.compile(
    r"[ăâđêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩị"
    r"óòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]",
    re.IGNORECASE,
)


def simple_lang_detect(text: str) -> str:
    return "vi" if _VI_REGEX.search(text or "") else "en"


def scores_to_labels(scores: dict, thr_map: dict) -> dict:
    labs = {k: int(scores.get(k, 0.0) >= thr_map.get(k, 0.5)) for k in LABELS}
    # nếu severe_toxic = 1 thì auto toxic = 1
    if labs.get("severe_toxic", 0) == 1:
        labs["toxic"] = 1
    return labs


def decide(labels: dict) -> str:
    if labels.get("identity_hate") == 1 or labels.get("threat") == 1:
        return "block"
    return "review" if any(labels.values()) else "allow"


def run_en(text: str):
    raw = predict_toxic(text)  # dict {label: percent}
    scores = {k: float(raw.get(k, 0.0)) / 100.0 for k in LABELS}
    thr = THRESHOLDS["en"]
    labels = scores_to_labels(scores, thr)
    return {
        "lang": "en",
        "scores": scores,
        "thresholds": thr,
        "labels": labels,
        "model": {"used": "en-toxic-lr"},
        "decision": decide(labels),
    }


def run_vi(text: str):
    # Nếu model VN không load được thì fallback sang EN
    if not VN_MODEL_AVAILABLE:
        print("[info] VN model not available, fallback to EN pipeline.")
        res = run_en(text)
        res["lang"] = "vi_fallback_en"
        return res

    out_list = predict_texts([text])  # [{probs: {...}}]
    item = out_list[0]
    scores = item["probs"]
    thr = THRESHOLDS["vi"]
    labels = scores_to_labels(scores, thr)
    return {
        "lang": "vi",
        "scores": scores,
        "thresholds": thr,
        "labels": labels,
        "model": {"used": "vi-toxic-xlm-r"},
        "decision": decide(labels),
    }


@app.route("/predict", methods=["POST"])
def predict_toxic_api():
    data = request.get_json(force=True) or {}
    text = (data.get("text") or "").strip()
    lang = (data.get("lang") or "auto").lower()

    if not text:
        return jsonify({"error": "Missing 'text'"}), 400

    if lang == "auto":
        lang = simple_lang_detect(text)  # "vi" hoặc "en"

    try:
        if lang == "vi":
            res = run_vi(text)
        elif lang == "en":
            res = run_en(text)
        else:
            res = run_en(text)

        labels = res.get("labels", {})
        out = {k: int(bool(labels.get(k, 0))) for k in LABELS}

        return jsonify(out)
    except Exception as e:
        print("Error in /predict:", e)
        return jsonify({"error": "internal_error"}), 500


@app.route("/summarize", methods=["POST"])
def summarize_api():
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    ext = os.path.splitext(file.filename)[-1] or ".pdf"

    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        file.save(tmp.name)
        summary = summarize_text(tmp.name)

    return jsonify({"description": summary})


@app.route("/extract-text", methods=["POST"])
def extract_text_api():
    if "file" not in request.files:
        return jsonify({"error": "No file provided"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "Empty filename"}), 400

    # Lấy tên file gốc (nếu có) và mimetype
    filename = secure_filename(file.filename or "")
    mimetype = (file.mimetype or "").lower()
    ext = os.path.splitext(filename)[-1].lower()

    # Đoán lại ext nếu thiếu / sai dựa trên mimetype
    if ext not in [".pdf", ".docx"]:
        if "pdf" in mimetype:
            ext = ".pdf"
        elif "word" in mimetype or "officedocument.wordprocessingml.document" in mimetype:
            ext = ".docx"
        else:
            # nếu vẫn không đoán được thì giả định PDF
            ext = ".pdf"

    print(f"[extract-text] filename={filename!r}, mimetype={mimetype!r}, use ext={ext!r}")

    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        file.save(tmp.name)
        tmp_path = tmp.name

    try:
        text = extract_n_sentences(tmp_path)
    except Exception as e:
        print("[extract-text][ERROR]", e)
        return jsonify({"error": str(e)}), 500
    finally:
        try:
            os.remove(tmp_path)
        except:
            pass

    return jsonify({"text": text})


@app.route("/extract-images", methods=["POST"])
def extract_images_api():
    if "file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    file = request.files["file"]
    print("[extract-text] filename from client:", repr(file.filename), "mimetype:", file.mimetype)
    filename = secure_filename(file.filename)

    temp_dir = tempfile.gettempdir()
    temp_path = os.path.join(temp_dir, filename)
    file.save(temp_path)

    try:
        images = extract_images_from_pdf(temp_path)

        tmp_dir = tempfile.mkdtemp()
        image_paths = []
        result = []

        for name, data in images:
            img_path = os.path.join(tmp_dir, name)
            with open(img_path, "wb") as f:
                f.write(data)
            image_paths.append(img_path)

            encoded = base64.b64encode(data).decode("utf-8")
            result.append(
                {
                    "filename": name,
                    "image_base64": encoded,
                }
            )

        vecs = get_latent_features(image_paths, model, transformations)
        if vecs is None:
            return jsonify({"error": "No features extracted"}), 400

        for i, vec in enumerate(vecs):
            result[i]["features"] = vec.tolist()

        return jsonify({"images": result})
    finally:
        if os.path.exists(temp_path):
            os.remove(temp_path)


db_features = load_features_from_db()
print(f"Loaded {len(db_features)} image features into memory.")


@app.route("/refresh-features", methods=["POST"])
def refresh_features():
    global db_features
    db_features = load_features_from_db()
    return jsonify({"message": f"Reloaded {len(db_features)} features from DB"})


@app.route("/add-features", methods=["POST"])
def add_features():
    global db_features
    data = request.get_json()

    if not data or "items" not in data:
        return jsonify({"error": "Missing 'items' in body"}), 400

    count = 0
    for item in data["items"]:
        try:
            feature_vec = np.array(item["featureVector"], dtype=np.float32)
            db_features.append(
                {
                    "id": item["id"],
                    "imagePath": item["imagePath"],
                    "documentId": item["documentId"],
                    "featureVector": feature_vec,
                }
            )
            count += 1
        except Exception as e:
            print("Error adding feature:", e)

    return jsonify(
        {
            "message": f"Added {count} features",
            "total": len(db_features),
        }
    )


@app.route("/search-image", methods=["POST"])
def search_image_api():
    if "file" not in request.files:
        return jsonify({"error": "No image file uploaded"}), 400

    file = request.files["file"]
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp:
        file.save(tmp.name)

    try:
        query_features = get_latent_features_img(tmp.name, model, transformations)
        if query_features is None:
            return jsonify({"error": "Failed to extract features from image"}), 500

        results = perform_search(query_features, db_features)
        return jsonify({"results": results})
    finally:
        if os.path.exists(tmp.name):
            os.remove(tmp.name)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
