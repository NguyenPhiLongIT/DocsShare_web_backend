from flask import Flask, request, jsonify, Blueprint
from urllib.parse import unquote
import pickle
import os
import sys
from collections import defaultdict

import numpy as np

try:
    import numpy._core.numeric as _  # noqa: F401
except ImportError:
    pass

import faiss
from sentence_transformers import SentenceTransformer


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# Lên một cấp từ semantic/ để đến flask_server/
FLASK_SERVER_DIR = os.path.dirname(BASE_DIR)

# File semantic_search_model.pkl nằm trong thư mục models
MODEL_PKL_PATH = os.path.join(FLASK_SERVER_DIR, "models", "semantic_search_model.pkl")
TOPIC_VECTORS_CACHE_PATH = os.path.join(FLASK_SERVER_DIR, "models", "topic_vectors_cache.pkl")

print("Loading semantic search model from:", MODEL_PKL_PATH)


def _ensure_numpy_modules():
    try:
        import numpy._core
        import numpy._core.multiarray
        import numpy._core.numeric
        import numpy._core.umath
    except (ImportError, AttributeError):
        # Nếu không có _core (numpy cũ), bỏ qua
        pass


_state = None
_last_model_mtime = None

config = {}
df_kw = None
kw_index = None

doc_vecs = None
doc_ids_arr = None
cat_ids_arr = None

doc_full_vecs = None
doc_full_ids = None
doc_meta = None
doc_full_map = {}

# Topic cache trong RAM
_topic_vectors = None
_topic_names = None
_topic_index = None
_topic_ids_list = None

# Lazy loading SentenceTransformer
_model = None
EMBEDDING_MODEL_NAME = None


def load_semantic_state_if_needed():
    global _state, _last_model_mtime
    global config, df_kw, kw_index
    global doc_vecs, doc_ids_arr, cat_ids_arr
    global doc_full_vecs, doc_full_ids, doc_meta, doc_full_map
    global _topic_vectors, _topic_names, _topic_index, _topic_ids_list
    global EMBEDDING_MODEL_NAME

    if not os.path.exists(MODEL_PKL_PATH):
        raise FileNotFoundError(f"semantic_search_model.pkl not found at {MODEL_PKL_PATH}")

    mtime = os.path.getmtime(MODEL_PKL_PATH)

    if _state is not None and _last_model_mtime is not None and mtime <= _last_model_mtime:
        # Không có thay đổi mới
        return

    # Có thay đổi / lần đầu load
    print("\n[semantic] Reloading semantic_search_model.pkl ...")
    print(f"          mtime = {mtime}, last_mtime = {_last_model_mtime}")

    _ensure_numpy_modules()

    try:
        with open(MODEL_PKL_PATH, "rb") as f:
            state = pickle.load(f)
    except (ModuleNotFoundError, AttributeError) as e:
        print(f"❌ Error loading model: {e}")
        print("💡 This is likely due to numpy version incompatibility.")
        print(f"   Current numpy version: {np.__version__}")
        raise RuntimeError(
            f"Cannot load model due to numpy compatibility issue: {e}\n"
            "Please try reinstalling numpy or re-pickle the model."
        ) from e

    _state = state
    _last_model_mtime = mtime

    # Khôi phục cấu hình & data
    config = state["config"]
    EMBEDDING_MODEL_NAME = config["embedding_model_name"]

    # Keywords dataset
    df_kw_local = state["df_kw"]
    df_kw_local = df_kw_local  # nếu cần clone, có thể thêm .copy()
    # Keyword index
    kw_index_bytes = state["kw_index_bytes"]
    kw_index_local = faiss.deserialize_index(kw_index_bytes)

    # Doc vectors
    doc_vecs_local = state["doc_vecs"].astype("float32")
    doc_ids_arr_local = state["doc_ids_arr"].astype("int64")
    cat_ids_arr_local = state["cat_ids_arr"].astype("int64")

    # Full doc vectors
    doc_full_vecs_local = state["doc_full_vecs"].astype("float32")
    doc_full_ids_local = state["doc_full_ids"].astype("int64")

    doc_meta_local = state["doc_meta"]

    doc_full_map_local = {
        int(doc_full_ids_local[i]): doc_full_vecs_local[i]
        for i in range(len(doc_full_ids_local))
    }

    # Gán về globals
    global df_kw, kw_index
    df_kw = df_kw_local
    kw_index = kw_index_local

    global doc_summary_vecs
    doc_summary_vecs = {}

    global doc_full_vecs, doc_full_ids, doc_meta, doc_vecs, doc_ids_arr, cat_ids_arr, doc_full_map
    doc_full_vecs = doc_full_vecs_local
    doc_full_ids = doc_full_ids_local
    doc_meta = doc_meta_local

    doc_vecs = doc_vecs_local
    doc_ids_arr = doc_ids_arr_local
    cat_ids_arr = cat_ids_arr_local

    doc_full_map = doc_full_map_local

    # Tạo doc_summary_vecs (check norm nếu muốn)
    for i in range(len(doc_full_ids_local)):
        doc_id = int(doc_full_ids_local[i])
        doc_vec = doc_full_vecs_local[i].astype("float32")
        vec_norm = np.linalg.norm(doc_vec)
        if abs(vec_norm - 1.0) > 0.01:
            print(f"⚠️  Warning: Document {doc_id} vector norm = {vec_norm:.4f} (expected ~1.0)")
        doc_summary_vecs[doc_id] = doc_vec

    # Khi state thay đổi, xoá topic cache trong RAM (file cache đã được script embed xóa)
    _topic_vectors = None
    _topic_names = None
    _topic_index = None
    _topic_ids_list = None

    print("[semantic] Model loaded:")
    print("   #keywords           :", len(df_kw))
    print("   #chunks (n-grams)   :", doc_vecs.shape[0])
    print("   #docs (full)        :", len(doc_full_ids))
    print("   embedding_model_name:", EMBEDDING_MODEL_NAME)


def get_model():
    """Lazy load SentenceTransformer model - chỉ load khi cần thiết."""
    global _model, EMBEDDING_MODEL_NAME
    if EMBEDDING_MODEL_NAME is None:
        # Đảm bảo state đã được load để biết EMBEDDING_MODEL_NAME
        load_semantic_state_if_needed()
    if _model is None:
        print("🔄 Đang load embedding model (lần đầu tiên)...")
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
        print("✅ Model đã được load thành công")
    return _model


def embed_query(texts):
    """Embed query text - model sẽ được load tự động nếu chưa có."""
    model = get_model()
    texts = [f"query: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")


def embed_passage(texts):
    """Embed passage text - model sẽ được load tự động nếu chưa có."""
    model = get_model()
    texts = [f"passage: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")


# ====== BUILD TOPIC VECTORS (có cache, phụ thuộc df_kw) ======
def build_topic_vectors():
    """Lazy build topic vectors - chỉ build khi cần thiết, cache vào file."""
    global _topic_vectors, _topic_names, _topic_index, _topic_ids_list

    # Đảm bảo state & df_kw đã load
    load_semantic_state_if_needed()

    if _topic_vectors is not None:
        return _topic_vectors, _topic_names, _topic_index, _topic_ids_list

    # Thử load từ cache trước
    if os.path.exists(TOPIC_VECTORS_CACHE_PATH):
        try:
            print("📂 Đang load topic vectors từ cache...")
            with open(TOPIC_VECTORS_CACHE_PATH, "rb") as f:
                cache_data = pickle.load(f)
                _topic_vectors = cache_data["topic_vectors"]
                _topic_names = cache_data["topic_names"]
                topic_ids_list = cache_data["topic_ids_list"]

                # Rebuild FAISS index từ cached vectors
                topic_vecs_array = np.array(
                    [_topic_vectors[tid] for tid in topic_ids_list]
                ).astype("float32")
                _topic_index = faiss.IndexFlatIP(topic_vecs_array.shape[1])
                _topic_index.add(topic_vecs_array)
                _topic_ids_list = topic_ids_list

                print(f"   ✅ Đã load {len(_topic_vectors)} topic vectors từ cache")
                return _topic_vectors, _topic_names, _topic_index, _topic_ids_list
        except Exception as e:
            print(f"   ⚠️  Không thể load cache: {e}, sẽ build lại...")

    # Nếu không có cache, build mới
    print("🔄 Đang build topic vectors từ keywords (lần đầu tiên)...")
    print("   ⏳ Quá trình này có thể mất vài phút...")

    topic_keyword_dict = defaultdict(list)  # {topic_id: [(keyword, vector), ...]}

    # Batch embed keywords để tăng tốc
    all_keywords = []
    keyword_meta = []

    for idx, row in df_kw.iterrows():
        topic_id = int(row["category_id"])
        keyword = str(row["keyword"])
        all_keywords.append(keyword)
        keyword_meta.append(
            {
                "topic_id": topic_id,
                "category_name": str(row.get("category_name", "")),
            }
        )

    print(f"   📝 Đang embed {len(all_keywords)} keywords...")
    all_vectors = embed_passage(all_keywords)

    for keyword, vector, meta in zip(all_keywords, all_vectors, keyword_meta):
        topic_id = meta["topic_id"]
        topic_keyword_dict[topic_id].append(
            {
                "keyword": keyword,
                "vector": vector,
                "category_name": meta["category_name"],
            }
        )

    print(f"   ✅ Đã embed xong, đang tính topic vectors...")

    topic_vectors = {}
    topic_names = {}

    for topic_id, keywords in topic_keyword_dict.items():
        if not keywords:
            continue

        topic_names[topic_id] = keywords[0]["category_name"]

        kw_vectors = []
        for kw in keywords:
            kw_vec = kw["vector"].astype("float32")
            kw_vectors.append(kw_vec)

        kw_vectors_array = np.array(kw_vectors)
        topic_vector = np.mean(kw_vectors_array, axis=0).astype("float32")

        vec_norm = np.linalg.norm(topic_vector)
        if vec_norm > 0:
            topic_vector = topic_vector / vec_norm

        topic_vectors[topic_id] = topic_vector.astype("float32")

    print(f"   ✅ Đã build {len(topic_vectors)} topic vectors")

    topic_ids_list = list(topic_vectors.keys())
    topic_vecs_array = np.array(
        [topic_vectors[tid] for tid in topic_ids_list]
    ).astype("float32")
    topic_index = faiss.IndexFlatIP(topic_vecs_array.shape[1])
    topic_index.add(topic_vecs_array)

    # Cache vào file
    try:
        print("💾 Đang lưu topic vectors vào cache...")
        cache_data = {
            "topic_vectors": topic_vectors,
            "topic_names": topic_names,
            "topic_ids_list": topic_ids_list,
        }
        with open(TOPIC_VECTORS_CACHE_PATH, "wb") as f:
            pickle.dump(cache_data, f)
        print("   ✅ Đã lưu cache thành công")
    except Exception as e:
        print(f"   ⚠️  Không thể lưu cache: {e}")

    _topic_vectors = topic_vectors
    _topic_names = topic_names
    _topic_index = topic_index
    _topic_ids_list = topic_ids_list

    return topic_vectors, topic_names, topic_index, topic_ids_list


# ====== SEARCH CORE (giữ logic như notebook) ======
def search_core(
    query: str,
    top_k_topics: int = None,          # sẽ dùng làm top_k_keywords
    top_k_docs_per_topic: int = None,  # số doc trả về
    sim_threshold: float = None,
):
    """
    Smart Search V2 – bám theo logic search_once() trong notebook.
    """

    # Đảm bảo state mới nhất đã được load (nếu .pkl có thay đổi)
    load_semantic_state_if_needed()

    if not query:
        return {"query": query, "results": [], "error": "EMPTY_QUERY"}

    query_clean = str(query).strip()
    if not query_clean:
        return {"query": query, "results": [], "error": "EMPTY_QUERY"}

    # Tham số mặc định từ config
    if sim_threshold is None:
        sim_threshold = float(config.get("sim_threshold_default", 0.3))

    if top_k_docs_per_topic is None:
        top_k_docs = int(config.get("top_k_docs_default", 10))
    else:
        top_k_docs = int(top_k_docs_per_topic)

    if top_k_topics is None:
        top_k_keywords = int(config.get("top_k_keywords_default", 5))
    else:
        top_k_keywords = int(top_k_topics)

    w_full = float(config.get("w_full_default", 0.6))
    w_local = float(config.get("w_local_default", 0.4))

    print("\n🔍 ====== SMART SEARCH (notebook logic) ======")
    print(f"   Query: '{query_clean}'")
    print(
        f"   sim_threshold = {sim_threshold}, top_k_keywords = {top_k_keywords}, top_k_docs = {top_k_docs}"
    )
    print(f"   w_full = {w_full}, w_local = {w_local}")

    # 1) Embed query
    q_vec = embed_query([query_clean]).astype("float32")  # (1, dim)
    print(
        f"   ✅ Query vector shape: {q_vec.shape}, norm={np.linalg.norm(q_vec):.4f}"
    )

    # 2) Đoán category bằng keyword (kw_index)
    D_kw, I_kw = kw_index.search(q_vec, top_k_keywords)
    best_pos = int(I_kw[0][0])
    best_sim = float(D_kw[0][0])

    best_kw_row = df_kw.iloc[best_pos]
    cat_id = int(best_kw_row["category_id"])
    cat_name = str(best_kw_row["category_name"])
    kw_text = str(best_kw_row["keyword"])

    print("\n🎯 Dự đoán category theo keyword:")
    print(f"   - category_id   : {cat_id}")
    print(f"   - category_name : {cat_name}")
    print(f"   - keyword match : \"{kw_text}\"")
    print(f"   - keyword_sim   : {best_sim:.3f}")

    # 3) Lấy tất cả n-gram thuộc category đó
    mask = cat_ids_arr == cat_id
    if not mask.any():
        print("\n⚠ Category này chưa có n-gram nào (không có tài liệu).")
        return {
            "query": query_clean,
            "sim_threshold": sim_threshold,
            "results": [
                {
                    "topic_id": cat_id,
                    "topic_name": cat_name,
                    "keyword_match": kw_text,
                    "topic_similarity": best_sim,
                    "documents": [],
                }
            ],
        }

    vecs_cat = doc_vecs[mask].astype("float32")  # (N_chunk, dim)
    doc_ids_cat = doc_ids_arr[mask].astype("int64")  # (N_chunk,)
    print(f"   📄 Số chunk (n-gram) trong category {cat_id}: {vecs_cat.shape[0]}")

    # 4) Tính similarity query - từng n-gram
    sim_q = (vecs_cat @ q_vec.T).reshape(-1)  # (N_chunk,)

    # 5) Gộp theo doc_id, chỉ giữ max_sim_q
    doc_scores: dict[int, dict] = {}
    for chunk_sim_q, doc_id_val in zip(sim_q, doc_ids_cat):
        if float(chunk_sim_q) < sim_threshold:
            continue

        doc_id_int = int(doc_id_val)
        val = float(chunk_sim_q)

        if doc_id_int not in doc_scores:
            doc_scores[doc_id_int] = {"max_sim_q": val}
        else:
            if val > doc_scores[doc_id_int]["max_sim_q"]:
                doc_scores[doc_id_int]["max_sim_q"] = val

    if not doc_scores:
        print("\n  (Không có doc nào đủ similarity theo threshold)")
        return {
            "query": query_clean,
            "sim_threshold": sim_threshold,
            "results": [
                {
                    "topic_id": cat_id,
                    "topic_name": cat_name,
                    "keyword_match": kw_text,
                    "topic_similarity": best_sim,
                    "documents": [],
                }
            ],
        }

    # 6) Thêm sim_full và sim_doc_dense
    for doc_id_int, vals in doc_scores.items():
        doc_full_vec = doc_full_map.get(doc_id_int)
        if doc_full_vec is None:
            sim_full = vals["max_sim_q"]  # fallback
        else:
            sim_full = float(doc_full_vec @ q_vec.T)  # cosine

        vals["sim_full"] = sim_full
        vals["sim_doc_dense"] = w_full * sim_full + w_local * vals["max_sim_q"]

    # 7) Sắp xếp theo sim_doc_dense
    sorted_docs = sorted(
        doc_scores.items(), key=lambda x: x[1]["sim_doc_dense"], reverse=True
    )

    # 8) Lấy metadata
    meta_list = doc_meta.get(str(cat_id)) or doc_meta.get(int(cat_id), [])
    meta_by_id = {int(m["doc_id"]): m for m in meta_list}

    documents = []
    for doc_id_int, vals in sorted_docs[:top_k_docs]:
        m = meta_by_id.get(doc_id_int, {})
        documents.append(
            {
                "doc_id": int(doc_id_int),
                "title": m.get("title", ""),
                "summary": m.get("summary", m.get("description", "")),
                "similarity_full": vals["sim_full"],
                "similarity_ngram": vals["max_sim_q"],
                "similarity": vals["sim_doc_dense"],
            }
        )

    print(f"\n📚 Top {len(documents)} documents trong category {cat_id}:")
    for i, d in enumerate(documents[:5], 1):
        print(
            f"   {i}. [{d['doc_id']}] {d['title'][:60]}...  (sim={d['similarity']:.3f})"
        )

    result = {
        "query": query_clean,
        "sim_threshold": sim_threshold,
        "results": [
            {
                "topic_id": cat_id,
                "topic_name": cat_name,
                "keyword_match": kw_text,
                "topic_similarity": best_sim,
                "documents": documents,
            }
        ],
    }
    return result


# ====== FLASK API ======
app = Flask(__name__)

# Route cũ dạng /semantic-search (nếu đang dùng)
@app.route("/semantic-search", methods=["POST"])
def semantic_search():
    data = request.get_json(force=True) or {}
    query = data.get("query", "")
    top_k_topics = data.get("top_k_topics")
    top_k_docs_per_topic = data.get("top_k_docs_per_topic")
    sim_threshold = data.get("sim_threshold")

    res = search_core(
        query=query,
        top_k_topics=top_k_topics,
        top_k_docs_per_topic=top_k_docs_per_topic,
        sim_threshold=sim_threshold,
    )

    return jsonify(res)


# Blueprint cho /semantic/search và /semantic/embed-doc
semantic_bp = Blueprint("semantic", __name__)


@semantic_bp.route("/semantic/search", methods=["GET", "POST"])
def semantic_search_endpoint():
    if request.method == "GET":
        query_raw = request.args.get("query", "")
        if query_raw and "%" in query_raw:
            try:
                query = unquote(query_raw, encoding="utf-8")
                if "%" in query:
                    query = unquote(query, encoding="utf-8")
            except Exception as e:
                print(f"⚠️  Lỗi khi unquote query: {e}, dùng query gốc")
                query = query_raw
        else:
            query = query_raw

        top_k_topics = request.args.get("top_k_topics")
        top_k_docs_per_topic = request.args.get("top_k_docs_per_topic")
        sim_threshold = request.args.get("sim_threshold")

        if top_k_topics:
            top_k_topics = int(top_k_topics)
        if top_k_docs_per_topic:
            top_k_docs_per_topic = int(top_k_docs_per_topic)
        if sim_threshold:
            sim_threshold = float(sim_threshold)
    else:
        data = request.get_json(force=True) or {}
        query = data.get("query", "")
        top_k_topics = data.get("top_k_topics")
        top_k_docs_per_topic = data.get("top_k_docs_per_topic")
        sim_threshold = data.get("sim_threshold")

    print(f"🔍 Semantic Search Request:")
    if request.method == "GET":
        query_raw_for_log = request.args.get("query", "")
        print(f"   Raw query (from URL): '{query_raw_for_log}'")
        print(f"   Processed query (after decode): '{query}'")
    else:
        query_raw_for_log = data.get("query", "")
        print(f"   Raw query (from JSON): '{query_raw_for_log}'")
        print(f"   Processed query: '{query}'")
    print(f"   Query length: {len(query)}, bytes: {len(query.encode('utf-8'))}")
    print(f"   Query repr: {repr(query)}")
    print(
        f"   top_k_topics={top_k_topics}, top_k_docs_per_topic={top_k_docs_per_topic}, sim_threshold={sim_threshold}"
    )

    try:
        res = search_core(
            query=query,
            top_k_topics=top_k_topics,
            top_k_docs_per_topic=top_k_docs_per_topic,
            sim_threshold=sim_threshold,
        )
        print(f"✅ Search completed: {len(res.get('results', []))} topics found")
        return jsonify(res)
    except Exception as e:
        print(f"❌ Error in semantic search: {e}")
        import traceback

        traceback.print_exc()
        return (
            jsonify({"query": query, "error": str(e), "results": []}),
            500,
        )


@semantic_bp.route("/semantic/embed-doc", methods=["POST"])
def embed_doc_endpoint():
    """
    Endpoint cho script update_semantic_model.py gọi sang.
    Dùng chung model + embed_passage, không load model mới.
    """
    data = request.get_json(force=True) or {}

    summary = data.get("summary", "")
    chunks = data.get("chunks", [])

    if not isinstance(chunks, list):
        return jsonify({"error": "chunks must be a list of strings"}), 400

    # Đảm bảo string sạch
    summary = str(summary) if summary is not None else ""
    chunks = [str(c) for c in chunks]

    try:
        # Dùng chung model + hàm embed_passage
        summary_vec = None
        if summary.strip():
            summary_vec = embed_passage([summary])[0].tolist()

        chunk_vecs = []
        if chunks:
            chunk_vecs = embed_passage(chunks).tolist()

        return jsonify({
            "summary_vec": summary_vec,
            "chunk_vecs": chunk_vecs,
        })
    except Exception as e:
        print(f"❌ Error in /semantic/embed-doc: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


app.register_blueprint(semantic_bp)


if __name__ == "__main__":
    # Đảm bảo lần đầu chạy cũng load state (để in log model)
    load_semantic_state_if_needed()
    app.run(host="0.0.0.0", port=5000)
