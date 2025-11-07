import pickle
import numpy as np
import mysql.connector

conn = mysql.connector.connect(
    host="localhost",
    port=3306,
    user="root",
    password="Long10092003",
    database="docsshare_db"
)
cursor = conn.cursor(dictionary=True)
cursor.execute("SELECT id, image_path, document_id, feature_vector FROM document_image")

features = []
for row in cursor.fetchall():
    features.append({
        "id": row["id"],
        "imagePath": row["image_path"],
        "documentId": row["document_id"],
        "featureVector": np.array(eval(row["feature_vector"]))
    })

with open("features.pkl", "wb") as f:
    pickle.dump(features, f)

print(f"✅ Saved {len(features)} features to features.pkl")
