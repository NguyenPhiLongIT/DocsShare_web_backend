from flask import Flask, request, jsonify
from toxic.service import predict_toxic
from summary.service import summarize_text

app = Flask(__name__)

@app.route("/predict", methods=["POST"])
def predict_api():
    data = request.get_json()
    text = data.get("text", "")
    return jsonify(predict_toxic(text))

@app.route("/summarize", methods=["POST"])
def summarize_api():
    data = request.get_json()
    text = data.get("text", "")
    return jsonify(summarize_text(text))

if __name__ == "__main__":
    app.run(debug=True)
