"""
SEMANTIC SEARCH MODULE
=====================
QUAN TRỌNG: Module này chỉ SỬ DỤNG model, KHÔNG THAY ĐỔI model.

Model được load từ semantic_search_model.pkl với:
- doc_full_vecs: Vectors của documents (đã normalize bởi model khi tạo)
- df_kw: Keywords dataset cho từng topic
- doc_meta: Metadata của documents

INPUT FORMAT (theo thiết kế model):
- Query: embed với prefix "query:" 
- Keywords/Passages: embed với prefix "passage:"
- normalize_embeddings=True trong model.encode()

OUTPUT FORMAT:
- Trả về danh sách topics với documents, mỗi item có similarity score
- Format phù hợp với SemanticSearchService.java DTO

KHÔNG THAY ĐỔI:
- Cách normalize vectors (model đã normalize)
- Input format (query:/passage: prefix)
- Output format (DTO structure)
- Embedding model settings
"""

from flask import Flask, request, jsonify
from urllib.parse import unquote
import pickle
import os
import sys
from collections import defaultdict

# Import numpy trước để đảm bảo module được load đầy đủ
import numpy as np
# Force import numpy._core để tránh lỗi khi unpickle
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

print("Loading semantic search model from:", MODEL_PKL_PATH)

# Workaround cho numpy compatibility issue
# Đảm bảo tất cả numpy submodules được import trước khi unpickle
def _ensure_numpy_modules():
    """Đảm bảo các numpy modules cần thiết được import"""
    try:
        # Import các modules numpy có thể cần
        import numpy._core
        import numpy._core.multiarray
        import numpy._core.numeric
        import numpy._core.umath
    except (ImportError, AttributeError):
        # Nếu không có _core (numpy cũ), bỏ qua
        pass

_ensure_numpy_modules()

try:
    with open(MODEL_PKL_PATH, "rb") as f:
        state = pickle.load(f)
except (ModuleNotFoundError, AttributeError) as e:
    # Lỗi do numpy version mismatch
    print(f"❌ Error loading model: {e}")
    print("💡 This is likely due to numpy version incompatibility.")
    print(f"   Current numpy version: {np.__version__}")
    print("\n🔧 Solutions:")
    print("   1. Try reinstalling numpy:")
    print("      pip uninstall numpy")
    print("      pip install 'numpy<2.0'")
    print("   2. Or try with specific version that matches the model:")
    print("      pip install numpy==1.24.3  # Example")
    print("   3. Re-pickle the model with current numpy version")
    raise RuntimeError(
        f"Cannot load model due to numpy compatibility issue: {e}\n"
        "Please try reinstalling numpy or re-pickle the model."
    ) from e


config = state["config"]
EMBEDDING_MODEL_NAME = config["embedding_model_name"]
DEFAULT_SIM_THRESHOLD = float(config.get("sim_threshold_default", 0.2))  # Giảm threshold mặc định từ 0.3 xuống 0.2
DEFAULT_TOP_K_TOPICS = int(config.get("top_k_topics_default", 5))  # Tăng số topics tìm từ 3 lên 5
DEFAULT_TOP_K_DOCS = int(config.get("top_k_docs_default", 10))

print(f"⚙️  Default settings:")
print(f"   - Sim threshold: {DEFAULT_SIM_THRESHOLD}")
print(f"   - Top K topics: {DEFAULT_TOP_K_TOPICS}")
print(f"   - Top K docs per topic: {DEFAULT_TOP_K_DOCS}")

print("Embedding model:", EMBEDDING_MODEL_NAME)
print("💡 Model sẽ được load lazy (chỉ khi có request đầu tiên)")

# Lazy loading: Model sẽ chỉ được load khi có request đầu tiên
_model = None

def get_model():
    """Lazy load model - chỉ load khi cần thiết"""
    global _model
    if _model is None:
        print("🔄 Đang load embedding model (lần đầu tiên)...")
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
        print("✅ Model đã được load thành công")
    return _model

def embed_query(texts):
    """Embed query text - model sẽ được load tự động nếu chưa có"""
    model = get_model()
    texts = [f"query: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")

def embed_passage(texts):
    """Embed passage text - model sẽ được load tự động nếu chưa có"""
    model = get_model()
    texts = [f"passage: {t}" for t in texts]
    return model.encode(texts, normalize_embeddings=True).astype("float32")


# ====== KHÔI PHỤC CÁC BIẾN TỪ .PKL ======
# (cấu trúc đúng như notebook đã save)
config          = state["config"]
df_kw           = state["df_kw"]

# FAISS keyword index (deserialize từ bytes)
kw_index_bytes  = state["kw_index_bytes"]
kw_index        = faiss.deserialize_index(kw_index_bytes)

# N-gram vectors theo category
doc_vecs        = state["doc_vecs"].astype("float32")    # (n_chunks, dim)
doc_ids_arr     = state["doc_ids_arr"].astype("int64")   # doc_id cho từng chunk
cat_ids_arr     = state["cat_ids_arr"].astype("int64")   # category_id cho từng chunk

# Vector full doc (summary / description)
doc_full_vecs   = state["doc_full_vecs"].astype("float32")  # (n_docs, dim)
doc_full_ids    = state["doc_full_ids"].astype("int64")
doc_meta        = state["doc_meta"]   # {category_id -> list[meta]}

# Map doc_id -> full vector (để dùng nhanh trong search)
doc_full_map = {
    int(doc_full_ids[i]): doc_full_vecs[i]
    for i in range(len(doc_full_ids))
}

print("Model loaded:")
print("   #keywords:", len(df_kw))
print("   #chunks (n-grams):", doc_vecs.shape[0])
print("   #docs (full):", len(doc_full_ids))


# Map doc_id -> full vector (summary vector)
# QUAN TRỌNG: Không normalize lại - vectors từ model đã được normalize khi tạo
# doc_full_vecs đã được tạo với normalize_embeddings=True trong model.encode()
# Nếu normalize lại sẽ làm sai kết quả similarity
doc_summary_vecs = {}
for i in range(len(doc_full_ids)):
    doc_id = int(doc_full_ids[i])
    doc_vec = doc_full_vecs[i].astype("float32")
    # Kiểm tra xem vector đã được normalize chưa (norm ≈ 1.0)
    vec_norm = np.linalg.norm(doc_vec)
    if abs(vec_norm - 1.0) > 0.01:  # Nếu norm khác 1.0 nhiều, có thể chưa normalize
        print(f"⚠️  Warning: Document {doc_id} vector norm = {vec_norm:.4f} (expected ~1.0)")
    doc_summary_vecs[doc_id] = doc_vec

print("Model loaded:")
print("   #keywords:", len(df_kw))
print("   #docs with summaries:", len(doc_summary_vecs))

# ====== XÂY DỰNG TỪ ĐIỂN KEYWORD THEO CHỦ ĐỀ ======
# Lazy loading: Chỉ build topic vectors khi cần thiết (lần đầu search)
_topic_vectors = None
_topic_names = None
_topic_index = None
_topic_ids_list = None

# File cache cho topic vectors
TOPIC_VECTORS_CACHE_PATH = os.path.join(FLASK_SERVER_DIR, "models", "topic_vectors_cache.pkl")

def build_topic_vectors():
    """Lazy build topic vectors - chỉ build khi cần thiết, cache vào file"""
    global _topic_vectors, _topic_names, _topic_index, _topic_ids_list
    
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
                topic_vecs_array = np.array([_topic_vectors[tid] for tid in topic_ids_list]).astype("float32")
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
    
    # Mỗi chủ đề (category) có một tập hợp các keyword với vector tương ứng
    topic_keyword_dict = defaultdict(list)  # {topic_id: [(keyword, vector), ...]}

    # Batch embed keywords để tăng tốc (embed nhiều keywords cùng lúc)
    all_keywords = []
    keyword_meta = []  # Lưu metadata để map lại
    
    for idx, row in df_kw.iterrows():
        topic_id = int(row["category_id"])
        keyword = str(row["keyword"])
        all_keywords.append(keyword)
        keyword_meta.append({
            "topic_id": topic_id,
            "category_name": str(row.get("category_name", ""))
        })
    
    # Batch embed tất cả keywords cùng lúc (nhanh hơn nhiều)
    print(f"   📝 Đang embed {len(all_keywords)} keywords...")
    all_vectors = embed_passage(all_keywords)
    
    # Map vectors về topics
    for i, (keyword, vector, meta) in enumerate(zip(all_keywords, all_vectors, keyword_meta)):
        topic_id = meta["topic_id"]
        topic_keyword_dict[topic_id].append({
            "keyword": keyword,
            "vector": vector,
            "category_name": meta["category_name"]
        })
    
    print(f"   ✅ Đã embed xong, đang tính topic vectors...")

    # Tạo vector đại diện cho mỗi topic (trung bình các keyword vectors)
    # Mỗi topic có một tập hợp keywords với vector tương ứng
    topic_vectors = {}
    topic_names = {}
    topic_keyword_vectors = {}  # Lưu từ điển keyword vectors cho mỗi topic
    
    for topic_id, keywords in topic_keyword_dict.items():
        if keywords:
            # Lấy tên category từ keyword đầu tiên
            topic_names[topic_id] = keywords[0]["category_name"]
            
            # Tính vector trung bình của tất cả keywords trong topic
            # QUAN TRỌNG: Vectors từ embed_passage() đã được normalize (normalize_embeddings=True)
            # Không normalize lại để đảm bảo tính nhất quán với model
            kw_vectors = []
            keyword_dict = {}  # Từ điển keyword -> vector cho topic này
            
            for kw in keywords:
                kw_vec = kw["vector"].astype("float32")
                # Vector đã được normalize trong embed_passage(), không normalize lại
                kw_vectors.append(kw_vec)
                keyword_dict[kw["keyword"]] = kw_vec
            
            # Lưu từ điển keyword vectors cho topic
            topic_keyword_vectors[topic_id] = keyword_dict
            
            # Tính vector trung bình (vectors đã normalize từ model)
            kw_vectors_array = np.array(kw_vectors)
            topic_vector = np.mean(kw_vectors_array, axis=0).astype("float32")
            
            # QUAN TRỌNG: Normalize lại vector trung bình
            # Vector trung bình của các normalized vectors không nhất thiết là normalized
            # Cần normalize để đảm bảo tính nhất quán khi tính cosine similarity
            vec_norm = np.linalg.norm(topic_vector)
            if vec_norm > 0:
                topic_vector = topic_vector / vec_norm
            topic_vectors[topic_id] = topic_vector.astype("float32")

    print(f"   ✅ Đã build {len(topic_vectors)} topic vectors")

    # Tạo FAISS index cho topic vectors để tìm topic phù hợp nhanh
    topic_ids_list = list(topic_vectors.keys())
    topic_vecs_array = np.array([topic_vectors[tid] for tid in topic_ids_list]).astype("float32")
    topic_index = faiss.IndexFlatIP(topic_vecs_array.shape[1])  # Inner Product for cosine similarity
    topic_index.add(topic_vecs_array)
    
    # Cache vào file để lần sau không phải build lại
    try:
        print("💾 Đang lưu topic vectors vào cache...")
        cache_data = {
            "topic_vectors": topic_vectors,
            "topic_names": topic_names,
            "topic_ids_list": topic_ids_list
        }
        with open(TOPIC_VECTORS_CACHE_PATH, "wb") as f:
            pickle.dump(cache_data, f)
        print("   ✅ Đã lưu cache thành công")
    except Exception as e:
        print(f"   ⚠️  Không thể lưu cache: {e}")
    
    # Cache trong memory
    _topic_vectors = topic_vectors
    _topic_names = topic_names
    _topic_index = topic_index
    _topic_ids_list = topic_ids_list
    
    return topic_vectors, topic_names, topic_index, topic_ids_list


# ====== HÀM SEARCH CORE MỚI ======
def search_core(query: str,
                top_k_topics: int = None,          # sẽ dùng làm top_k_keywords
                top_k_docs_per_topic: int = None,  # số doc trả về
                sim_threshold: float = None):
    """
    Smart Search V2 – bám theo logic search_once() trong notebook.

    - Bước 1: Đoán category bằng keyword gần nhất (kw_index).
    - Bước 2: Chỉ lấy n-gram của category đó, tính max_sim_q theo doc.
    - Bước 3: Kết hợp sim_full + max_sim_q -> sim_doc_dense = w_full*sim_full + w_local*max_sim_q.
    - FE vẫn nhận format:
        {
          query,
          sim_threshold,
          results: [
            {
              topic_id,
              topic_name,
              topic_similarity,   # = keyword_sim
              documents: [
                { doc_id, title, summary, similarity, ... }
              ]
            }
          ]
        }
    """

    # 0) Validate + clean query
    if not query:
        return {
            "query": query,
            "results": [],
            "error": "EMPTY_QUERY"
        }

    query_clean = str(query).strip()
    if not query_clean:
        return {
            "query": query,
            "results": [],
            "error": "EMPTY_QUERY"
        }

    # Các tham số mặc định từ config notebook
    if sim_threshold is None:
        sim_threshold = float(config.get("sim_threshold_default", 0.3))
    if top_k_docs_per_topic is None:
        top_k_docs = DEFAULT_TOP_K_DOCS
    else:
        top_k_docs = int(top_k_docs_per_topic)
    # notebook dùng top_k_keywords, map từ top_k_topics
    if top_k_topics is None:
        top_k_keywords = 5
    else:
        top_k_keywords = int(top_k_topics)

    w_full  = float(config.get("w_full_default", 0.6))
    w_local = float(config.get("w_local_default", 0.4))

    print("\n🔍 ====== SMART SEARCH (notebook logic) ======")
    print(f"   Query: '{query_clean}'")
    print(f"   sim_threshold = {sim_threshold}, top_k_keywords = {top_k_keywords}, top_k_docs = {top_k_docs}")
    print(f"   w_full = {w_full}, w_local = {w_local}")

    # 1) Embed query
    q_vec = embed_query([query_clean]).astype("float32")  # (1, dim)
    print(f"   ✅ Query vector shape: {q_vec.shape}, norm={np.linalg.norm(q_vec):.4f}")

    # 2) Đoán category bằng keyword (kw_index)
    D_kw, I_kw = kw_index.search(q_vec, top_k_keywords)
    best_pos = int(I_kw[0][0])
    best_sim = float(D_kw[0][0])

    best_kw_row = df_kw.iloc[best_pos]
    cat_id   = int(best_kw_row["category_id"])
    cat_name = str(best_kw_row["category_name"])
    kw_text  = str(best_kw_row["keyword"])

    print(f"\n🎯 Dự đoán category theo keyword:")
    print(f"   - category_id   : {cat_id}")
    print(f"   - category_name : {cat_name}")
    print(f"   - keyword match : \"{kw_text}\"")
    print(f"   - keyword_sim   : {best_sim:.3f}")

    # 3) Lấy tất cả n-gram thuộc category đó
    mask = (cat_ids_arr == cat_id)
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
                    "documents": []
                }
            ]
        }

    vecs_cat    = doc_vecs[mask].astype("float32")     # (N_chunk, dim)
    doc_ids_cat = doc_ids_arr[mask].astype("int64")    # (N_chunk,)
    print(f"   📄 Số chunk (n-gram) trong category {cat_id}: {vecs_cat.shape[0]}")

    # 4) Tính similarity query - từng n-gram (cosine = dot vì vector đã normalize)
    sim_q = (vecs_cat @ q_vec.T).reshape(-1)           # (N_chunk,)

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
                    "documents": []
                }
            ]
        }

    # 6) Thêm sim_full và sim_doc_dense = w_full * sim_full + w_local * max_sim_q
    for doc_id_int, vals in doc_scores.items():
        doc_full_vec = doc_full_map.get(doc_id_int)
        if doc_full_vec is None:
            sim_full = vals["max_sim_q"]    # fallback đơn giản
        else:
            sim_full = float(doc_full_vec @ q_vec.T)  # cosine

        vals["sim_full"]      = sim_full
        vals["sim_doc_dense"] = w_full * sim_full + w_local * vals["max_sim_q"]

    # 7) Sắp xếp theo sim_doc_dense
    sorted_docs = sorted(
        doc_scores.items(),
        key=lambda x: x[1]["sim_doc_dense"],
        reverse=True
    )

    # 8) Lấy metadata theo category + doc_id
    meta_list = doc_meta.get(str(cat_id)) or doc_meta.get(int(cat_id), [])
    meta_by_id = {int(m["doc_id"]): m for m in meta_list}

    documents = []
    for doc_id_int, vals in sorted_docs[:top_k_docs]:
        m = meta_by_id.get(doc_id_int, {})
        documents.append({
            "doc_id": int(doc_id_int),
            "title": m.get("title", ""),
            "summary": m.get("summary", m.get("description", "")),
            # 3 loại similarity nếu sau này muốn debug:
            "similarity_full":   vals["sim_full"],
            "similarity_ngram":  vals["max_sim_q"],
            "similarity":        vals["sim_doc_dense"],   # FE đang dùng field này
        })

    print(f"\n📚 Top {len(documents)} documents trong category {cat_id}:")
    for i, d in enumerate(documents[:5], 1):
        print(f"   {i}. [{d['doc_id']}] {d['title'][:60]}...  (sim={d['similarity']:.3f})")

    # 9) Format JSON trả cho FE (1 topic + danh sách documents)
    result = {
        "query": query_clean,
        "sim_threshold": sim_threshold,
        "results": [
            {
                "topic_id": cat_id,
                "topic_name": cat_name,
                "keyword_match": kw_text,
                "topic_similarity": best_sim,  # dùng keyword_sim
                "documents": documents
            }
        ]
    }

    return result



# ====== FLASK API ======
app = Flask(__name__)

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


# Blueprint để register vào app chính
from flask import Blueprint
semantic_bp = Blueprint('semantic', __name__)

@semantic_bp.route("/semantic/search", methods=["GET", "POST"])
def semantic_search_endpoint():
    # Hỗ trợ cả GET và POST
    if request.method == "GET":
        # Flask tự động decode URL encoding từ URLEncoder.encode() trong Java
        # Nhưng cần kiểm tra xem có cần unquote thêm không
        query_raw = request.args.get("query", "")
        # Kiểm tra nếu query có dấu % (có thể chưa được decode)
        # Nếu có, unquote một lần nữa (tránh double decode)
        if query_raw and '%' in query_raw:
            # Có thể cần unquote thêm nếu Flask chưa decode hết
            try:
                query = unquote(query_raw, encoding='utf-8')
                # Nếu sau khi unquote vẫn còn %, có thể là double encoding
                if '%' in query:
                    query = unquote(query, encoding='utf-8')
            except Exception as e:
                print(f"⚠️  Lỗi khi unquote query: {e}, dùng query gốc")
                query = query_raw
        else:
            # Flask đã decode rồi, dùng trực tiếp
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

    # QUAN TRỌNG: Log query để kiểm tra input vào model
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
    print(f"   Query repr: {repr(query)}")  # Hiển thị đầy đủ ký tự đặc biệt
    print(f"   top_k_topics={top_k_topics}, top_k_docs_per_topic={top_k_docs_per_topic}, sim_threshold={sim_threshold}")
    
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
        return jsonify({
            "query": query,
            "error": str(e),
            "results": []
        }), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5005)
