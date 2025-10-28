import numpy as np
import joblib
import os, re, string
from pathlib import Path
# import nltk
# nltk.download('punkt')
# nltk.download('wordnet')
# nltk.download('stopwords')
from nltk.corpus import stopwords
stop_words = set(stopwords.words("english"))
from nltk.tokenize import word_tokenize
from nltk.stem import WordNetLemmatizer
from sklearn.feature_extraction.text import TfidfVectorizer 
from scipy.sparse import hstack

PKG_DIR = Path(__file__).resolve().parent
MODEL_DIR = Path(os.getenv("TOXIC_MODEL_DIR", PKG_DIR / "models")).resolve()

def _load_joblib(name: str):
    p = MODEL_DIR / name
    if not p.exists():
        raise FileNotFoundError(f"Không tìm thấy: {p} (cwd={Path.cwd()})")
    return joblib.load(str(p))

word_tfidf = _load_joblib("word_tfidf_vectorizer.pkl")
char_tfidf = _load_joblib("char_tfidf_vectorizer.pkl")

lr_toxic = _load_joblib("logistic_regression_toxic.pkl")
lr_severe = _load_joblib("logistic_regression_severe_toxic.pkl")
lr_obscene = _load_joblib("logistic_regression_obscene.pkl")
lr_threat = _load_joblib("logistic_regression_threat.pkl")
lr_insult = _load_joblib("logistic_regression_insult.pkl")
lr_identity = _load_joblib("logistic_regression_identity_hate.pkl")


# Creating a function to clean the training dataset
def clean_text(text):
    """This function will take text as input and return a cleaned text 
        by removing html char, punctuations, non-letters, newline and converting it 
        to lower case.
    """
    # Converting to lower case letters
    text = text.lower()
    # Removing the contraction of few words
    text = re.sub(r"what's", "what is ", text)
    text = re.sub(r"\'s", " ", text)
    text = re.sub(r"\'ve", " have ", text)
    text = re.sub(r"can't", "can not ", text)
    text = re.sub(r"n't", " not ", text)
    text = re.sub(r"i'm", "i am ", text)
    text = re.sub(r"\'re", " are ", text)
    text = re.sub(r"\'d", " would ", text)
    text = re.sub(r"\'ll", " will ", text)
    text = re.sub(r"\'scuse", " excuse ", text)
    # Replacing the HTMl characters with " "
    text = re.sub("<.*?>", " ", text)
    # Removing the punctuations
    text = text.translate(str.maketrans(" ", " ", string.punctuation))
    # Removing non-letters
    text = re.sub("[^a-zA-Z]", " ", text)
    # Replacing newline with space
    text = re.sub("\n", " ", text)
    # Split on space and rejoin to remove extra spaces
    text = " ".join(text.split())
    
    return text

def word_lemmatizer(text):
    """This function will help lemmatize words in a text.
    """
    
    lemmatizer = WordNetLemmatizer()
    # Tokenize the sentences to words
    text = word_tokenize(text)
    # Removing the stop words
    text = [lemmatizer.lemmatize(word) for word in text]
    # Joining the cleaned list
    text = " ".join(text)
    
    return text

def predict_toxic(text_input):
    text_cleaned = clean_text(text_input)
    text_data = word_lemmatizer(text_cleaned)
    
    input = [text_data]
    
    # Transforming to TF-IDF vectors
    word_features = word_tfidf.transform(input)
    char_features = char_tfidf.transform(input)
    all_features = hstack([word_features, char_features])
    
    result = {
        "comment_text": text_input,
        "toxic": float(np.round(lr_toxic.predict_proba(all_features)[:,1], 2)[0] * 100),
        "severe_toxic": float(np.round(lr_severe.predict_proba(all_features)[:,1], 2)[0] * 100),
        "obscene": float(np.round(lr_obscene.predict_proba(all_features)[:,1], 2)[0] * 100),
        "threat": float(np.round(lr_threat.predict_proba(all_features)[:,1], 2)[0] * 100),
        "insult": float(np.round(lr_insult.predict_proba(all_features)[:,1], 2)[0] * 100),
        "identity_hate": float(np.round(lr_identity.predict_proba(all_features)[:,1], 2)[0] * 100),
    }

    return result 

# # For AWS
if __name__ == '__main__':
    text = "This is a sample comment text!"
    output = predict_toxic(text)
    print(output)