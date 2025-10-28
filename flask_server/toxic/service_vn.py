import os
from typing import List, Optional, Dict
import torch
import numpy as np
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

LABELS = ["toxic","severe_toxic","obscene","threat","insult","identity_hate"]
MODEL_DIR = str(BASE_DIR / "model_vn")
MAX_LEN   = int(os.environ.get("MAX_LEN", "256"))
DEVICE    = "cuda" if torch.cuda.is_available() else "cpu"

THRESHOLDS = {
    "toxic": 0.8,
    "severe_toxic": 0.55,
    "obscene": 0.8,
    "threat": 0.8,
    "insult": 0.8,
    "identity_hate": 0.9,
}
PRESET_THRESHOLDS: Dict[str, Dict[str, float]] = {
    "lenient":  {c: 0.50 for c in LABELS},
    "balanced": {"toxic":0.80,"severe_toxic":0.85,"obscene":0.90,"threat":0.85,"insult":0.85,"identity_hate":0.90},
    "strict":   {"toxic":0.85,"severe_toxic":0.90,"obscene":0.95,"threat":0.90,"insult":0.90,"identity_hate":0.95},
}

THR_FLOOR = 0.20  # sàn ngưỡng an toàn

print(f"[info] Loading model from: {MODEL_DIR} (device={DEVICE})")
tok = AutoTokenizer.from_pretrained(MODEL_DIR)
model = AutoModelForSequenceClassification.from_pretrained(MODEL_DIR).to(DEVICE)
model.eval()
torch.set_num_threads(1)

def _resolve_thresholds_from_const(
    preset: Optional[str] = None,
    default: float = 0.5
) -> Dict[str, float]:
    """
    Trả về map ngưỡng per-label.
    - Nếu có preset hợp lệ → dùng preset.
    - Ngược lại dùng THRESHOLDS trong code.
    - Mọi ngưỡng đều áp sàn THR_FLOOR.
    """
    src = PRESET_THRESHOLDS.get(preset, THRESHOLDS)
    return {c: max(float(src.get(c, default)), THR_FLOOR) for c in LABELS}

@torch.inference_mode()
def predict_texts(
    texts: List[str],
    preset: Optional[str] = None,
    top_k: Optional[int] = None,
    abstain_below: Optional[float] = None,  # ví dụ 0.20 → từ chối nếu max prob < 0.20
) -> List[dict]:
    if isinstance(texts, str):
        texts = [texts]

    # Encode + forward
    batch = tok(
        texts, truncation=True, padding=True,
        max_length=MAX_LEN, return_tensors="pt"
    ).to(DEVICE)

    logits = model(**batch).logits
    probs = torch.sigmoid(logits).cpu().numpy()  # (B, C)

    thr_map = _resolve_thresholds_from_const(preset=preset, default=0.5)

    outs = []
    for i, txt in enumerate(texts):
        p = {LABELS[j]: float(probs[i, j]) for j in range(len(LABELS))}

        # 1) Abstain nếu cấu hình
        if abstain_below is not None and max(p.values()) < float(abstain_below):
            outs.append({
                "text": txt,
                "probs": p,
                "labels": {},
                "thresholds": thr_map,
                "decision": "abstain"
            })
            continue

        # 2) Suy luận theo ngưỡng hoặc top_k
        if top_k is not None and top_k > 0:
            top = sorted(p.items(), key=lambda kv: kv[1], reverse=True)[:top_k]
            labs = {c: 1 if c in dict(top) else 0 for c in LABELS}
        else:
            labs = {c: int(p[c] >= thr_map[c]) for c in LABELS}

        # 3) Ràng buộc logic: severe_toxic ⇒ toxic
        if labs.get("severe_toxic", 0) == 1:
            labs["toxic"] = 1

        outs.append({
            "text": txt,
            "probs": p,
            "labels": labs,
        })
    return outs

if __name__ == "__main__":
    sample_text = "tài liệu này thật sâu sắc và hữu ích"
    print(predict_texts([sample_text]))