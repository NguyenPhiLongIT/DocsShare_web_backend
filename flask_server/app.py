import os
from flask import Flask, request, jsonify
import tempfile
import torch
import pickle
import base64
import numpy as np
from werkzeug.utils import secure_filename
from toxic.service import predict_toxic
from summary.service import summarize_text
from cbir.extract_img import extract_images_from_pdf
from cbir.service import load_model, transformations, get_latent_features, get_latent_features_img, perform_search

app = Flask(__name__)

MODEL_PATH = "cbir/conv_autoencoderv2_200ep_3.pt"
model = load_model(MODEL_PATH)

@app.route("/predict", methods=["POST"])
def predict_api():
    data = request.get_json()
    text = data.get("text", "")
    return jsonify(predict_toxic(text))

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

with open("cbir/features.pkl", "rb") as f:
    db_features = pickle.load(f)
print(f"Loaded {len(db_features)} image features into memory.")
print(f"🔍 Example item keys: {list(db_features[0].keys())}")

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
