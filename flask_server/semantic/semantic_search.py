from flask import request, jsonify, Blueprint
from urllib.parse import unquote
import os
import pickle
import numpy as np
import faiss
from sentence_transformers import SentenceTransformer
from collections import defaultdict
from typing import Optional



BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FLASK_SERVER_DIR = os.path.dirname(BASE_DIR)

MODEL_PKL_PATH = os.path.join(FLASK_SERVER_DIR, "models", "semantic_search_model.pkl")

print("🔵 [semantic] Loading state from:", MODEL_PKL_PATH)


def load_embedding_model(model_name: str):
    print(f"🔵 [semantic] Loading embedding model: {model_name}")
    model = SentenceTransformer(model_name)
    print("   ✅ Model loaded.")
    return model

state = {
    "config": None,
    "df_kw": None,
    "kw_index": None,
    "doc_vecs": None,
    "doc_ids_arr": None,
    "cat_ids_arr": None,
    "doc_full_vecs": None,
    "doc_full_ids": None,
    "doc_meta": None,
    "doc_full_map": {},
}

embedding_model = None  # SentenceTransformer instance


def load_state_once():
    global state, embedding_model

    if not os.path.exists(MODEL_PKL_PATH):
        raise FileNotFoundError(f"❌ semantic_search_model.pkl not found: {MODEL_PKL_PATH}")

    print("🔵 [semantic] Loading semantic_search_model.pkl (one-time)...")

    with open(MODEL_PKL_PATH, "rb") as f:
        raw = pickle.load(f)
    # Giữ lại toàn bộ raw state gốc
    state.update(raw)

    # Config
    state["config"] = raw["config"]
    embed_name = state["config"]["embedding_model_name"]

    # Load embedding model (only one time)
    if embedding_model is None:
        embedding_model = load_embedding_model(embed_name)

    # Keywords dataset
    state["df_kw"] = raw["df_kw"]

    # Lưu luôn kw_index_bytes để dùng khi ghi file
    state["kw_index_bytes"] = raw.get("kw_index_bytes")

    # Keyword FAISS index (dùng bytes để build index trong RAM)
    state["kw_index"] = faiss.deserialize_index(raw["kw_index_bytes"])

    # n-gram vectors
    state["doc_vecs"] = raw["doc_vecs"].astype("float32")
    state["doc_ids_arr"] = raw["doc_ids_arr"].astype("int64")
    state["cat_ids_arr"] = raw["cat_ids_arr"].astype("int64")

    # Full document vectors
    state["doc_full_vecs"] = raw["doc_full_vecs"].astype("float32")
    state["doc_full_ids"] = raw["doc_full_ids"].astype("int64")

    # Metadata
    state["doc_meta"] = raw["doc_meta"]

    # Map id → vector
    state["doc_full_map"] = {
        int(doc_id): state["doc_full_vecs"][i]
        for i, doc_id in enumerate(state["doc_full_ids"])
    }

    print("   ✅ Loaded state:")
    print("      - #keywords     :", len(state["df_kw"]))
    print("      - #chunks       :", state["doc_vecs"].shape[0])
    print("      - #docs(full)   :", len(state["doc_full_ids"]))
    print("      - embedding model:", embed_name)

# Gọi load 1 lần khi import file
load_state_once()


def embed_query(texts):
    """Embed query: prefix 'query:'"""
    if isinstance(texts, str):
        texts = [texts]
    model = embedding_model
    texts = [f"query: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")

def embed_passage(texts):
    """Embed passage: prefix 'passage:'"""
    if isinstance(texts, str):
        texts = [texts]
    model = embedding_model
    texts = [f"passage: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")


config = state["config"]
df_kw = state["df_kw"]
kw_index = state["kw_index"]

doc_vecs = state["doc_vecs"]
doc_ids_arr = state["doc_ids_arr"]
cat_ids_arr = state["cat_ids_arr"]

doc_full_vecs = state["doc_full_vecs"]
doc_full_ids = state["doc_full_ids"]
doc_meta = state["doc_meta"]
doc_full_map = state["doc_full_map"]


def chunkify(text: str, max_chars: int = 600):

    import re

    if not text:
        return []

    sentences = [s.strip() for s in re.split(r"(?<=[.!?])\s+", text) if s.strip()]
    if not sentences:
        return [text.strip()]

    chunks = []
    current = []
    current_len = 0

    for sentence in sentences:
        sentence_len = len(sentence) + 1
        if current and current_len + sentence_len > max_chars:
            chunks.append(" ".join(current).strip())
            current = [sentence]
            current_len = len(sentence)
        else:
            current.append(sentence)
            current_len += sentence_len

    if current:
        chunks.append(" ".join(current).strip())

    return chunks


def append_document_to_state(
    doc_id: int,
    title: Optional[str],
    summary: str,
    description: Optional[str],
    category_id: Optional[int],
):


    global state
    global doc_vecs, doc_ids_arr, cat_ids_arr
    global doc_full_vecs, doc_full_ids, doc_full_map, doc_meta

    # Chuẩn hóa input
    cat_value = int(category_id) if category_id is not None else -1
    description = description or summary or ""
    summary = summary or description

    # 1) Tạo chunks từ description
    chunks = chunkify(description)
    if not chunks:
        chunks = [summary]

    # 2) Embed summary & chunks (passage)
    summary_vec = embed_passage([summary])[0]  # shape (dim,)
    chunk_vecs = embed_passage(chunks)         # shape (n_chunk, dim)

    # 3) Xác định dimension
    if doc_full_vecs is not None and doc_full_vecs.size > 0:
        dim = doc_full_vecs.shape[1]
    else:
        dim = summary_vec.shape[0]

    summary_vec = summary_vec.astype("float32").reshape(1, dim)
    chunk_vecs = chunk_vecs.astype("float32").reshape(-1, dim)

    # 4) Append vào doc_full_vecs + doc_full_ids
    if doc_full_vecs is None or doc_full_vecs.size == 0:
        doc_full_vecs = summary_vec
        doc_full_ids = np.array([doc_id], dtype="int64")
    else:
        doc_full_vecs = np.vstack([doc_full_vecs, summary_vec])
        doc_full_ids = np.append(doc_full_ids, [doc_id]).astype("int64")

    # 5) Append vào doc_vecs + doc_ids_arr + cat_ids_arr
    if chunk_vecs.size > 0:
        if doc_vecs is None or doc_vecs.size == 0:
            doc_vecs = chunk_vecs
            doc_ids_arr = np.full(len(chunk_vecs), doc_id, dtype="int64")
            cat_ids_arr = np.full(len(chunk_vecs), cat_value, dtype="int64")
        else:
            doc_vecs = np.vstack([doc_vecs, chunk_vecs])
            doc_ids_arr = np.append(
                doc_ids_arr,
                np.full(len(chunk_vecs), doc_id, dtype="int64"),
            )
            cat_ids_arr = np.append(
                cat_ids_arr,
                np.full(len(chunk_vecs), cat_value, dtype="int64"),
            )

    # 6) Cập nhật doc_full_map
    doc_full_map[int(doc_id)] = summary_vec[0]

    # 7) Cập nhật doc_meta
    if doc_meta is None:
        doc_meta = {}

    meta_entry = {
        "doc_id": int(doc_id),
        "title": title,
        "summary": summary,
        "description": description,
    }
    key = str(cat_value)
    doc_meta.setdefault(key, []).append(meta_entry)

    # 8) Ghi ngược lại vào state dict
    state["doc_vecs"] = doc_vecs
    state["doc_ids_arr"] = doc_ids_arr
    state["cat_ids_arr"] = cat_ids_arr
    state["doc_full_vecs"] = doc_full_vecs
    state["doc_full_ids"] = doc_full_ids
    state["doc_full_map"] = doc_full_map
    state["doc_meta"] = doc_meta

    # 9) Chuẩn bị state để ghi lại file .pkl (backup)
    save_state = dict(state)

    # Không ghi FAISS index object trực tiếp
    if "kw_index" in save_state:
        save_state.pop("kw_index")


    if "kw_index_bytes" not in save_state:
        raise RuntimeError("kw_index_bytes missing in state – cannot persist semantic model safely.")

    with open(MODEL_PKL_PATH, "wb") as f:
        pickle.dump(save_state, f, protocol=pickle.HIGHEST_PROTOCOL)

    print(
        f"✅ [semantic] Appended doc {doc_id} (cat={cat_value}) to state: "
        f"doc_full_vecs={doc_full_vecs.shape}, doc_vecs={doc_vecs.shape}"
    )


semantic_bp = Blueprint("semantic", __name__)


@semantic_bp.route("/semantic/embed-doc-internal", methods=["POST"])
def embed_doc_internal_endpoint():

    try:
        data = request.get_json(force=True) or {}

        doc_id = int(data["docId"])
        title = data.get("title")
        summary = data.get("summary") or data.get("description") or data.get("title") or ""
        description = data.get("description") or summary
        category_id = data.get("categoryId")

        print(f"🔵 [semantic] Embedding new document: id={doc_id}, cat={category_id}")

        append_document_to_state(
            doc_id=doc_id,
            title=title,
            summary=summary,
            description=description,
            category_id=category_id,
        )

        return jsonify({"message": f"Document {doc_id} embedded successfully."})
    except Exception as e:
        import traceback
        print("❌ [semantic] Error in /semantic/embed-doc-internal:", repr(e))
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


def search_core(
    query: str,
    top_k_topics: int = None,          # sẽ dùng làm top_k_keywords
    top_k_docs_per_topic: int = None,  # số doc trả về
    sim_threshold: float = None,
):


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
    q_vec = embed_query([query_clean])  # (1, dim)
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

    vecs_cat = doc_vecs[mask].astype("float32")      # (N_chunk, dim)
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
        import traceback
        print(f"❌ Error in semantic search: {e}")
        traceback.print_exc()
        return (
            jsonify({"query": query, "error": str(e), "results": []}),
            500,
        )

@semantic_bp.route("/semantic/embed-doc", methods=["POST"])
def embed_doc_endpoint():
    """
    Endpoint cũ cho script update_semantic_model.py gọi sang.
    Vẫn giữ nguyên behavior: trả về vector summary + chunk,
    nhưng KHÔNG tự append vào state (append dùng /semantic/embed-doc-internal).
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
        import traceback
        print(f"❌ Error in /semantic/embed-doc: {e}")
        traceback.print_exc()
        return jsonify({"error": str(e)}), 500


if __name__ == "__main__":
    from flask import Flask
    app = Flask(__name__)
    app.register_blueprint(semantic_bp)
    # Đảm bảo state đã load khi start (đã gọi load_state_once ở đầu file)
    app.run(host="0.0.0.0", port=5000)
