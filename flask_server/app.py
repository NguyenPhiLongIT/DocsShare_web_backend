import os, re
from flask import Flask, request, jsonify
import tempfile
import torch
import pickle
import base64
import numpy as np
from werkzeug.utils import secure_filename
from toxic.service import predict_toxic
from toxic.service_vn import predict_texts
from summary.service import summarize_text
from cbir.extract_img import extract_images_from_pdf
from cbir.export_features import load_features_from_db
from cbir.service import load_model, transformations, get_latent_features, get_latent_features_img, perform_search
from semantic.semantic_search import semantic_bp

app = Flask(__name__)
app.register_blueprint(semantic_bp)

MODEL_PATH = "cbir/conv_autoencoderv2_200ep_3.pt"
model = load_model(MODEL_PATH)

LABELS = ["toxic", "severe_toxic", "obscene", "threat", "insult", "identity_hate"]

THRESHOLDS = {
    "vi": {"toxic":0.8,"severe_toxic":0.8,"obscene":0.8,"threat":0.8,"insult":0.8,"identity_hate":0.8},
    "en": {"toxic":0.6,"severe_toxic":0.3,"obscene":0.6,"threat":0.3,"insult":0.6,"identity_hate":0.3},
}

_VI_REGEX = re.compile(r"[ăâđêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]", re.IGNORECASE)
def simple_lang_detect(text: str) -> str:
    return "vi" if _VI_REGEX.search(text or "") else "en"

def scores_to_labels(scores: dict, thr_map: dict) -> dict:
    labs = {k: int(scores.get(k, 0.0) >= thr_map.get(k, 0.5)) for k in LABELS}
    if labs.get("severe_toxic", 0) == 1:
        labs["toxic"] = 1
    return labs

def run_en(text: str):
    """
    predict_toxic() của bạn trả % 0..100; chuyển về 0..1 + map schema.
    """
    raw = predict_toxic(text)  
    scores = {k: float(raw.get(k, 0.0))/100.0 for k in LABELS}
    thr = THRESHOLDS["en"]
    labels = scores_to_labels(scores, thr)
    return {
        "lang": "en",
        "scores": scores,
        "labels": labels,
        "thresholds": thr,
        "model": {"used": "en-toxic-lr"},
        "decision": decide(labels)
    }

def run_vi(text: str):
    """
    predict_texts() của bạn trả list; lấy phần tử đầu, đã là 0..1.
    """
    out_list = predict_texts([text]) 
    item = out_list[0]
    scores = item["probs"]              
    thr = THRESHOLDS["vi"]
    labels = scores_to_labels(scores, thr)
    return {
        "lang": "vi",
        "scores": scores,
        "labels": labels,
        "thresholds": thr,
        "model": {"used": "vi-toxic-xlm-r"},
        "decision": decide(labels)
    }

def decide(labels: dict) -> str:
    if labels.get("identity_hate") == 1 or labels.get("threat") == 1:
        return "block"
    return "review" if any(labels.values()) else "allow"

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

        # scores = res.get("scores", {})  
        labels = res.get("labels", {})
        # percents = {k: round(float(scores.get(k, 0.0)) * 100, 2) for k in LABELS}
        out = {k: int(bool(labels.get(k, 0))) for k in LABELS}
        return jsonify(out)
    except Exception:
        return jsonify({"error": "internal_error"}), 500

@app.route("/summarize", methods=["POST"])
def summarize_api():
    if 'file' not in request.files:
        return jsonify({"error": "No file provided"}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "Empty filename"}), 400
    
    ext = os.path.splitext(file.filename)[-1] or ".pdf"

    with tempfile.NamedTemporaryFile(delete=False, suffix=ext) as tmp:
        file.save(tmp.name)
        summary = summarize_text(tmp.name)

    return jsonify({"description": summary})

@app.route("/extract-images", methods=["POST"])
def extract_images_api():
    if "file" not in request.files:
        return jsonify({"error": "No file uploaded"}), 400

    file = request.files["file"]
    filename = secure_filename(file.filename)

    # dùng thư mục tạm cross-platform
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
            result.append({
                "filename": name,
                "image_base64": encoded
            })

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
            db_features.append({
                "id": item["id"],
                "imagePath": item["imagePath"],
                "documentId": item["documentId"],
                "featureVector": feature_vec
            })
            count += 1
        except Exception as e:
            print("Error adding feature:", e)

    return jsonify({
        "message": f"Added {count} features",
        "total": len(db_features)
    })

@app.route("/search-image", methods=["POST"])
def search_image_api():
    if "file" not in request.files:
        return jsonify({"error": "No image file uploaded"}), 400

    file = request.files["file"]
    with tempfile.NamedTemporaryFile(delete=False, suffix=".jpg") as tmp:
        file.save(tmp.name) 

    query_features = get_latent_features_img(tmp.name, model, transformations)
    if query_features is None:
        return jsonify({"error": "Failed to extract features from image"}), 500
    
    results = perform_search(query_features, db_features)

    return jsonify({"results": results})

if __name__ == "__main__":
    app.run(debug=False)
