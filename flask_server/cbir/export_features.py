import ast
import numpy as np
import mysql.connector

def load_features_from_db():
    conn = mysql.connector.connect(
        host="localhost",
        port=3306,
        user="root",
        password="123456",
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
            "featureVector": np.array(ast.literal_eval(row["feature_vector"]))
        })
    
    cursor.close()
    conn.close()
    return features
