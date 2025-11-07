import os
from flask import Flask, request, jsonify
import tempfile
from toxic.service import predict_toxic
from summary.service import summarize_text
from semantic.semantic_search import semantic_bp


app = Flask(__name__)
app.register_blueprint(semantic_bp)

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

if __name__ == "__main__":
    app.run(debug=False)
