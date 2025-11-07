from flask import Blueprint, request, jsonify
from sentence_transformers import SentenceTransformer
import joblib, numpy as np, faiss, pandas as pd
from urllib.parse import unquote
import os, logging

semantic_bp = Blueprint("semantic", __name__)
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# nếu muốn thấy log ra console khi chạy trực tiếp:
if not logger.handlers:
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    fmt = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    ch.setFormatter(fmt)
    logger.addHandler(ch)

logger.info("🔁 Đang nạp semantic_search_model.pkl ...")

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_PATH = os.path.join(BASE_DIR, "models", "semantic_search_model.pkl")

# ===== Load bundle =====
bundle = joblib.load(MODEL_PATH)
model_name: str = bundle["model_name"]
model = SentenceTransformer(model_name)
index = bundle["faiss_index"]
metadata: pd.DataFrame = bundle["metadata"]

# Flags/params lưu từ lúc build index (khuyến nghị)
METRIC = bundle.get("metric_type", faiss.METRIC_INNER_PRODUCT)
VECTORS_NORMALIZED = bool(bundle.get("vectors_normalized", False))
USE_E5_PREFIX = bool(bundle.get("use_e5_prefix", False))

# Nếu IVF/HNSW, set params tìm kiếm lại
if hasattr(index, "nprobe"):
    index.nprobe = int(bundle.get("nprobe", getattr(index, "nprobe", 10)))
if hasattr(index, "hnsw"):
    index.hnsw.efSearch = int(bundle.get("hnsw_params", {}).get("efSearch", getattr(index.hnsw, "efSearch", 64)))

logger.info("✅ Semantic model sẵn sàng! model=%s | metric=%s | normalized=%s",
            model_name, str(METRIC), str(VECTORS_NORMALIZED))



def distance_to_similarity(distance: float) -> float:
    """
    Convert FAISS distance -> similarity [0,1], tôn trọng metric và trạng thái normalized
    """
    # METRIC_INNER_PRODUCT
    if METRIC == faiss.METRIC_INNER_PRODUCT:
        if VECTORS_NORMALIZED:
            # distance ~ cosine ∈ [-1, 1] → map về [0,1]
            return float(max(0.0, min(1.0, (distance + 1.0) / 2.0)))
        else:
            # Không normalized → logistic squash cho thứ hạng tương đối
            return float(1.0 / (1.0 + np.exp(-distance)))

    # METRIC_L2
    if METRIC == faiss.METRIC_L2:
        if VECTORS_NORMALIZED:
            # ||a-b||^2 = 2 - 2cos → cos = 1 - d^2/2 → map [-1,1]→[0,1]
            cos = 1.0 - (distance ** 2) / 2.0
            cos = max(-1.0, min(1.0, cos))
            return float((cos + 1.0) / 2.0)
        else:
            # Không normalized → nghịch đảo mượt
            return float(1.0 / (1.0 + max(1e-6, distance)))

    # Fallback
    return float(1.0 / (1.0 + max(1e-6, distance)))


@semantic_bp.route("/semantic/search", methods=["GET"])
def search():
    # --- lấy query ---
    raw_q = request.args.get("query", "")
    query = unquote(raw_q).strip()
    logger.info("🧠 Query nhận được: %s", query)

    if not query:
        return jsonify({"error": "Thiếu tham số query"}), 400

    # --- top_k ---
    try:
        req_top_k = int(request.args.get("top_k", 10))
    except Exception:
        req_top_k = 10

    ntotal = getattr(index, "ntotal", None)
    if ntotal is not None and ntotal > 0:
        top_k = max(1, min(req_top_k, ntotal))
    else:
        top_k = max(1, req_top_k)

    # --- encode query + (tuỳ) prefix E5 ---
    qtxt = f"query: {query}" if USE_E5_PREFIX else query
    q = model.encode([qtxt], convert_to_numpy=True)[0].astype("float32")

    # --- normalize query nếu IP + normalized ---
    if METRIC == faiss.METRIC_INNER_PRODUCT and VECTORS_NORMALIZED:
        qn = np.linalg.norm(q)
        if qn > 0:
            q = q / qn

    # --- search ---
    D, I = index.search(q.reshape(1, -1), k=top_k)

    raw_results = []
    ids = I[0]
    dists = D[0]

    for rank, (idx, dist) in enumerate(zip(ids, dists)):
        if idx < 0:
            continue
        if idx >= len(metadata):
            logger.warning("⚠️ FAISS trả về idx=%s vượt metadata size=%s", idx, len(metadata))
            continue

        row = metadata.iloc[int(idx)]
        sim = round(distance_to_similarity(float(dist)), 3)

        logger.info("🔍 Rank %d: idx=%s, dist=%.4f → sim=%.3f, category=%s",
                    rank, idx, float(dist), sim, row.get("category_name", "N/A"))

        raw_results.append({
            "category_id": int(row.get("category_id", 0)),
            "category_name": row.get("category_name"),
            "summary": row.get("summary", ""),
            "similarity": sim
        })

    # --- gộp trùng theo category_name: giữ điểm cao nhất ---
    best_by_name = {}
    for r in raw_results:
        cname = r.get("category_name")
        if not cname:
            continue
        if cname not in best_by_name or r["similarity"] > best_by_name[cname]["similarity"]:
            best_by_name[cname] = r

    # --- sort & lấy top 5 ---
    final_results = sorted(best_by_name.values(), key=lambda x: x["similarity"], reverse=True)[:5]

    # --- trả về ---
    return jsonify({
        "query": query,
        "top_k_requested": req_top_k,
        "top_k_effective": top_k,
        "metric": int(METRIC),                 # 0=IP, 1=L2
        "vectors_normalized": bool(VECTORS_NORMALIZED),
        "results": final_results
    })
