import re
import string
import numpy as np
import torch
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from transformers import AutoTokenizer, AutoModel
from .extract import extract_text

# Dùng GPU nếu có
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

def sentence_tokenize(text):
    return re.split(r'(?<=[.!?])\s+', text)

# ===== Tiền xử lý =====
def clean_text(text):
    """Clean and normalize Vietnamese text."""
    text = re.sub(r'\s+', ' ', text.strip())
    text = re.sub(r'https?://\S+|www\.\S+', '', text)
    text = re.sub(r'\s+', ' ', text.strip())
    return text

def simple_sentence_tokenize(text):
    """Simple regex-based sentence tokenizer for Vietnamese text."""
    sentences = re.split(r'(?<=[.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]

def preprocess_for_summary(text: str) -> str:
    # Bỏ ký tự lạ, mã hóa lỗi, xuống dòng quá nhiều
    text = text.replace("•", " ").replace("  ", " ")
    text = re.sub(r'\s{2,}', ' ', text)
    
    # Xóa câu lặp hoàn toàn
    lines = [line.strip() for line in text.split("\n") if line.strip()]
    unique_lines = list(dict.fromkeys(lines))  # preserve order, remove duplicates
    text = " ".join(unique_lines)
    
    return text


# ===== TF-IDF Summary =====
def tfidf_filter(text, ratio=0.3):
    sentences = simple_sentence_tokenize(clean_text(text))
    if len(sentences) < 3:
        return text

    vectorizer = TfidfVectorizer()
    try:
        sentence_vectors = vectorizer.fit_transform(sentences)
    except ValueError:
        return text

    sim_matrix = cosine_similarity(sentence_vectors)
    scores = np.sum(sim_matrix, axis=1)
    ranked = sorted(((scores[i], s) for i, s in enumerate(sentences)), reverse=True)

    top_k = max(1, int(len(sentences) * ratio))
    top_sentences = [s for _, s in ranked[:top_k]]
    return " ".join(top_sentences)



# ===== PhoBERT Summary =====
class PhoBERTSummarizer:
    def __init__(self):
        self.tokenizer = AutoTokenizer.from_pretrained("vinai/phobert-base")
        self.model = AutoModel.from_pretrained("vinai/phobert-base").to(device)

    def get_embeddings(self, sentences):
        embeddings = []
        for sentence in sentences:
            inputs = self.tokenizer(sentence, return_tensors="pt", truncation=True, padding=True, max_length=256).to(device)
            with torch.no_grad():
                outputs = self.model(**inputs)
            embeddings.append(outputs.last_hidden_state[:, 0, :].cpu().numpy()[0])
        return np.array(embeddings)

    def summarize(self, text, ratio=0.3):   # ratio: tỷ lệ câu được chọn (lớn thì tóm tắt dài hơn)
        sentences = simple_sentence_tokenize(clean_text(text))
        if len(sentences) < 3:
            return text

        embeddings = self.get_embeddings(sentences)
        sim_matrix = cosine_similarity(embeddings)
        scores = np.sum(sim_matrix, axis=1)
        
        ranked = sorted(((scores[i], i, s) for i, s in enumerate(sentences)), reverse=True)
        top_ids = sorted([i for _, i, _ in ranked[:max(1, int(len(sentences) * ratio))]])
        
        return " ".join([sentences[i] for i in top_ids])



# ===== Dịch vụ chính =====
phobert_summarizer = PhoBERTSummarizer()

def summarize_text(raw_text):
    text = extract_text(raw_text)
    preprocessed = preprocess_for_summary(text)
    # tfidf_selected = tfidf_filter(preprocessed, ratio=0.3)
    summary = phobert_summarizer.summarize(preprocessed, ratio=0.15)
    return summary