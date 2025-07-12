from transformers import (
    pipeline,
    AutoTokenizer,
    AutoModelForSeq2SeqLM
)
from langdetect import detect
import PyPDF2

# Load English summarization model (BART)
summarizer_en = pipeline("summarization", model="facebook/bart-large-cnn")

# Load Vietnamese summarization model (VietAI vit5)
tokenizer_vi = AutoTokenizer.from_pretrained("VietAI/vit5-large-vietnews-summarization")
model_vi = AutoModelForSeq2SeqLM.from_pretrained("VietAI/vit5-large-vietnews-summarization")

def summarize_vi(text: str) -> str:
    input_ids = tokenizer_vi.encode(text, return_tensors="pt", max_length=1024, truncation=True)
    summary_ids = model_vi.generate(input_ids, max_length=256, min_length=30, length_penalty=2.0, num_beams=4, early_stopping=True)
    return tokenizer_vi.decode(summary_ids[0], skip_special_tokens=True)

def summarize_text(text: str) -> str:
    """
    Auto-detect language and summarize text using appropriate model.
    """
    if not text.strip():
        return "Input text is empty."

    lang = detect(text[:1000])  # Detect language from first part

    if lang == 'vi':
        return summarize_vi(text)
    else:
        summary = summarizer_en(
            text,
            max_length=150,
            min_length=30,
            length_penalty=2.0,
            num_beams=4,
            early_stopping=True
        )
        return summary[0]['summary_text']

def extract_text_from_pdf(file_path: str) -> str:
    """
    Extract text from PDF file.
    """
    text = ""
    with open(file_path, 'rb') as f:
        reader = PyPDF2.PdfReader(f)
        for page in reader.pages:
            content = page.extract_text()
            if content:
                text += content + "\n"
    return text.strip()