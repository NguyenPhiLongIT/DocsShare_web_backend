import argparse
import json
import os
import pickle
import re
import sys
from pathlib import Path

import numpy as np
import requests

print("[embedding] === USING UPDATED SCRIPT v2 (debug input types, via Flask) ===")

BASE_DIR = Path(__file__).resolve().parent
MODEL_PATH = BASE_DIR.parent / "models" / "semantic_search_model.pkl"
TOPIC_CACHE_PATH = BASE_DIR.parent / "models" / "topic_vectors_cache.pkl"

# Có thể cấu hình URL Flask qua biến môi trường, mặc định dùng localhost:5000
FLASK_BASE_URL = os.getenv("SEMANTIC_FLASK_URL", "http://localhost:5000")


def clean_unicode(text) -> str:

    if text is None:
        return ""
    if not isinstance(text, str):
        text = str(text)
    # Loại bỏ surrogate / ký tự hỏng bằng cách re-encode
    return text.encode("utf-8", errors="ignore").decode("utf-8", errors="ignore")


def normalize_texts_for_encoding(texts):

    if texts is None:
        return []

    if isinstance(texts, (np.ndarray, set, tuple)):
        texts = list(texts)

    if not isinstance(texts, list):
        texts = [texts]

    cleaned = []
    for t in texts:
        if t is None:
            raw = ""
        elif isinstance(t, (dict, list, tuple, set, np.ndarray)):
            raw = json.dumps(t, ensure_ascii=False)
        else:
            raw = str(t)

        safe = clean_unicode(raw)
        if safe.strip() != "":
            cleaned.append(safe)

    return cleaned


def call_flask_embed(summary_text: str, chunks: list[str]):

    # Chuẩn hóa dữ liệu trước khi gửi
    summary_clean = clean_unicode(summary_text)
    chunks_clean = normalize_texts_for_encoding(chunks)

    url = f"{FLASK_BASE_URL}/semantic/embed-doc"
    payload = {
        "summary": summary_clean,
        "chunks": chunks_clean,
    }

    print(f"[embedding] Calling Flask embed endpoint: {url}")
    try:
        resp = requests.post(url, json=payload, timeout=120)
        resp.raise_for_status()
    except requests.RequestException as e:
        print("[embedding][ERROR] Failed to call Flask /semantic/embed-doc:", repr(e))
        raise

    data = resp.json()
    summary_vec = data.get("summary_vec")
    chunk_vecs = data.get("chunk_vecs") or []

    if summary_vec is None:
        raise ValueError("Flask /semantic/embed-doc did not return 'summary_vec'")

    return summary_vec, chunk_vecs


def chunkify(text: str, max_chars: int = 600):

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


def load_payload(args):

    if args.payload:
        with open(args.payload, "r", encoding="utf-8") as f:
            return json.load(f)
    raw = sys.stdin.read()
    if not raw.strip():
        raise ValueError("Embedding payload is required (stdin or --payload).")
    return json.loads(raw)


def main():
    parser = argparse.ArgumentParser(
        description="Update semantic search model with a new document."
    )
    parser.add_argument(
        "--doc-id",
        type=int,
        required=True,
        help="Document identifier (for logging only).",
    )
    parser.add_argument(
        "--payload",
        type=str,
        help="Optional path to payload JSON; defaults to stdin.",
    )
    args = parser.parse_args()

    if not MODEL_PATH.exists():
        raise FileNotFoundError(f"semantic_search_model.pkl not found at {MODEL_PATH}")

    payload = load_payload(args)

    # Lấy các trường từ payload
    target_doc_id = int(payload.get("docId") or args.doc_id)
    summary_text = (
        payload.get("summary")
        or payload.get("description")
        or payload.get("title")
        or ""
    )

    print(f"[embedding][DEBUG] doc_id        = {target_doc_id}")
    print(f"[embedding][DEBUG] summary type  = {type(summary_text)}")
    try:
        print(f"[embedding][DEBUG] summary text  = {repr(str(summary_text))[:200]}")
    except Exception:
        print("[embedding][DEBUG] summary text  = <unprintable>")

    if summary_text is None or str(summary_text).strip() == "":
        raise ValueError("Payload missing summary/description/title information.")

    content_text = payload.get("description") or summary_text
    category_id = payload.get("categoryId")

    with open(MODEL_PATH, "rb") as f:
        state = pickle.load(f)


    doc_full_ids = np.array(state["doc_full_ids"]).astype("int64")
    if target_doc_id in set(doc_full_ids.tolist()):
        print(f"[embedding] Document {target_doc_id} already embedded. Skipping.")
        return

    doc_vecs = np.array(state["doc_vecs"]).astype("float32")
    doc_ids_arr = np.array(state["doc_ids_arr"]).astype("int64")
    cat_ids_arr = np.array(state["cat_ids_arr"]).astype("int64")
    doc_full_vecs = np.array(state["doc_full_vecs"]).astype("float32")

    if doc_full_vecs.size == 0:
        raise ValueError("doc_full_vecs in model state is empty; cannot infer dimension.")
    dim = doc_full_vecs.shape[1]

    chunks = chunkify(content_text)
    if not chunks:
        chunks = [summary_text]

    summary_vec_list, chunk_vecs_list = call_flask_embed(summary_text, chunks)

    summary_vec = np.array(summary_vec_list, dtype="float32").reshape(1, dim)

    if chunk_vecs_list:
        chunk_vectors = np.array(chunk_vecs_list, dtype="float32").reshape(
            len(chunk_vecs_list), dim
        )
    else:
        chunk_vectors = np.zeros((0, dim), dtype="float32")

    doc_full_vecs = np.vstack([doc_full_vecs, summary_vec])
    doc_full_ids = np.append(doc_full_ids, [target_doc_id]).astype("int64")

    doc_vecs = np.vstack([doc_vecs, chunk_vectors])
    doc_ids_arr = np.append(
        doc_ids_arr, np.full(len(chunk_vectors), target_doc_id, dtype="int64")
    )

    cat_value = category_id if category_id is not None else -1
    cat_ids_arr = np.append(
        cat_ids_arr, np.full(len(chunk_vectors), cat_value, dtype="int64")
    )

    doc_meta = state.get("doc_meta", {})
    meta_entry = {
        "doc_id": target_doc_id,
        "title": payload.get("title"),
        "summary": summary_text,
        "description": content_text,
    }
    key = str(category_id) if category_id is not None else "uncategorized"
    doc_meta.setdefault(key, []).append(meta_entry)

    state["doc_vecs"] = doc_vecs
    state["doc_ids_arr"] = doc_ids_arr
    state["cat_ids_arr"] = cat_ids_arr
    state["doc_full_vecs"] = doc_full_vecs
    state["doc_full_ids"] = doc_full_ids
    state["doc_meta"] = doc_meta

    with open(MODEL_PATH, "wb") as f:
        pickle.dump(state, f)

    if TOPIC_CACHE_PATH.exists():
        os.remove(TOPIC_CACHE_PATH)
        print("[embedding] Removed topic cache to force rebuild on next query.")

    print(f"[embedding] Document {target_doc_id} embedded successfully (via Flask).")


if __name__ == "__main__":
    main()
