import re
import string
import numpy as np
import torch
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from transformers import AutoTokenizer, AutoModel
from .extract import extract_text
import os
os.environ["TRANSFORMERS_NO_TF"] = "1"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"


# Dùng GPU nếu có
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

def sentence_tokenize(text):
    return re.split(r'(?<=[.!?])\s+', text)

def clean_text(text):
    text = re.sub(r'\s+', ' ', text.strip())
    text = re.sub(r'https?://\S+|www\.\S+', '', text)
    text = re.sub(r'\s+', ' ', text.strip())
    return text

def simple_sentence_tokenize(text):
    sentences = re.split(r'(?<=[.!?])\s+', text)
    return [s.strip() for s in sentences if s.strip()]

def text_rank_reduce(sentences, keep_ratio):
    if len(sentences) < 10:
        return sentences  # tài liệu ngắn không cần giảm

    vectorizer = TfidfVectorizer(min_df=2, max_df=0.85)
    matrix = vectorizer.fit_transform(sentences)

    sim = cosine_similarity(matrix)
    scores = sim.sum(axis=1)

    ranked_ids = np.argsort(-scores)
    keep_n = max(5, int(len(sentences) * keep_ratio))

    selected = [sentences[i] for i in ranked_ids[:keep_n]]
    selected.sort()  # giữ thứ tự gốc

    return selected

class PhoBERTSummarizer:
    def __init__(self):
        self.tokenizer = AutoTokenizer.from_pretrained("vinai/phobert-base")
        self.model = AutoModel.from_pretrained("vinai/phobert-base").to(device)
        self.model.eval()

    def get_embeddings(self, sentences, batch_size=2): 
        all_embeddings = []

        for i in range(0, len(sentences), batch_size):
            batch = sentences[i:i+batch_size]

            inputs = self.tokenizer(
                batch,
                return_tensors="pt",
                truncation=True,
                padding=True,
                max_length=128   
            ).to(device)

            with torch.no_grad():
                outputs = self.model(**inputs)

            cls_embeds = outputs.last_hidden_state[:, 0, :].cpu().numpy()
            all_embeddings.append(cls_embeds)

        return np.vstack(all_embeddings)

    def summarize_sentences(self, sentences, ratio):
        if len(sentences) < 3:
            return " ".join(sentences)

        embeddings = self.get_embeddings(sentences)
        sim = cosine_similarity(embeddings)
        scores = np.sum(sim, axis=1)

        ranked = np.argsort(-scores)
        keep_n = max(1, int(len(sentences) * ratio))

        idx = sorted(ranked[:keep_n])
        return " ".join(sentences[i] for i in idx)

phobert_summarizer = PhoBERTSummarizer()

def summarize_text(file_path, mode="medium"):
    text = extract_text(file_path)
    raw = clean_text(text)
    sentences = simple_sentence_tokenize(raw)
    if mode == "short":
        keep_ratio = 0.4
        pho_ratio = 0.10
    elif mode == "long":
        keep_ratio = 0.7
        pho_ratio = 0.3
    else:  # medium
        keep_ratio = 0.5
        pho_ratio = 0.2
    sentences = text_rank_reduce(sentences, keep_ratio)
    summary = phobert_summarizer.summarize_sentences(sentences, pho_ratio)
    return summary