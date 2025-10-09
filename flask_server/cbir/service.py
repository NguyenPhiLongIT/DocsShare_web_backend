import torch
import torch.nn as nn
from torchvision import transforms
from PIL import Image
import numpy as np
import os

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

class ConvAutoencoder_v2(nn.Module):
    def __init__(self):
        super(ConvAutoencoder_v2, self).__init__()
        self.encoder = nn.Sequential(# in- (N,3,512,512)

            nn.Conv2d(in_channels=3,
                      out_channels=64,
                      kernel_size=(3,3),
                      stride=1,
                      padding=1),
            nn.ReLU(True),
            nn.Conv2d(in_channels=64,
                      out_channels=64,
                      kernel_size=(3,3),
                      stride=1,
                      padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(2, stride=2),

            nn.Conv2d(in_channels=64,
                      out_channels=128,
                      kernel_size=(3,3),
                      stride=2,
                      padding=1),
            nn.ReLU(True),
            nn.Conv2d(in_channels=128,
                      out_channels=128,
                      kernel_size=(3,3),
                      stride=1,
                      padding=0),
            nn.ReLU(True),
            nn.MaxPool2d(2, stride=2),

            nn.Conv2d(in_channels=128,
                      out_channels=256,
                      kernel_size=(3,3),
                      stride=2,
                      padding=1),
            nn.ReLU(True),
            nn.Conv2d(in_channels=256,
                      out_channels=256,
                      kernel_size=(3,3),
                      stride=1,
                      padding=1),
            nn.ReLU(True),
            nn.Conv2d(in_channels=256,
                      out_channels=256,
                      kernel_size=(3,3),
                      stride=1,
                      padding=1),
            nn.ReLU(True),
            nn.MaxPool2d(2, stride=2)
        )
        self.decoder = nn.Sequential(

            nn.ConvTranspose2d(in_channels = 256,
                               out_channels=256,
                               kernel_size=(3,3),
                               stride=1,
                              padding=1),

            nn.ConvTranspose2d(in_channels=256,
                               out_channels=256,
                               kernel_size=(3,3),
                               stride=1,
                               padding=1),
            nn.ReLU(True),

            nn.ConvTranspose2d(in_channels=256,
                               out_channels=128,
                               kernel_size=(3,3),
                               stride=2,
                               padding=0),

            nn.ConvTranspose2d(in_channels=128,
                               out_channels=64,
                               kernel_size=(3,3),
                               stride=2,
                               padding=1),
            nn.ReLU(True),
            nn.ConvTranspose2d(in_channels=64,
                               out_channels=32,
                               kernel_size=(3,3),
                               stride=2,
                               padding=1),

            nn.ConvTranspose2d(in_channels=32,
                               out_channels=32,
                               kernel_size=(3,3),
                               stride=2,
                               padding=1),
            nn.ReLU(True),

            nn.ConvTranspose2d(in_channels=32,
                               out_channels=3,
                               kernel_size=(4,4),
                               stride=2,
                               padding=2),
            nn.Tanh()
        )

    def forward(self, x):
        x = self.encoder(x)
        x = self.decoder(x)
        return x

def load_model(model_path):
    # 1. Khởi tạo model
    model = ConvAutoencoder_v2().to(device)

    # 2. Load checkpoint
    checkpoint = torch.load(model_path, map_location=device)

    # 3. Load state_dict
    if "model_state_dict" in checkpoint:
        model.load_state_dict(checkpoint["model_state_dict"])
    else:
        model.load_state_dict(checkpoint)

    # 4. Chuyển sang eval mode
    model.eval()
    return model

transformations = transforms.Compose([
    transforms.Resize((256, 256)),
    transforms.ToTensor(),
    transforms.Normalize((0.5, 0.5, 0.5), (0.5, 0.5, 0.5))
])

# Hàm lấy đặc trưng danh sách ảnh
def get_latent_features(images, model, transformations):
    features_list = []

    for image_path in images:
        try:
            img = Image.open(image_path).convert("RGB")
            tensor = transformations(img).unsqueeze(0).to(device)

            with torch.no_grad():
                latent = model.encoder(tensor)

            latent_np = latent.cpu().detach().numpy().squeeze(0).flatten()
            features_list.append(latent_np)

            del tensor, latent, latent_np, img
            torch.cuda.empty_cache()

        except Exception as e:
            print(f"⚠️ Bỏ ảnh lỗi: {image_path} ({e})")
            continue

    return np.stack(features_list, axis=0) if features_list else None

def get_latent_features_img(image_path, model, transformations):
    try:
        img = Image.open(image_path).convert("RGB")
        img = img.resize((256,256))
        tensor = transformations(img).unsqueeze(0).to(device)
        with torch.no_grad():
            latent = model.encoder(tensor).cpu().numpy().flatten()
        return latent
    except Exception as e:
        print(f"⚠️ Bỏ ảnh lỗi: {image_path} ({e})")
        return None

def euclidean(a, b):
    # compute and return the euclidean distance between two vectors
    return np.linalg.norm(a - b)

def perform_search(queryFeatures, db_features, threshold=0.7):
    results = []

    distances = []
    for i, item in enumerate(db_features):
        d = euclidean(queryFeatures, item["featureVector"])
        distances.append((d, i))

    max_d = max([d for d, _ in distances]) if distances else 1.0

    for d, i in distances:
        sim = 1 - (d / max_d)
        if sim >= threshold:
            results.append({
                "similarity": sim,
                "id": db_features[i]["id"],
                "imagePath": db_features[i]["image_path"],
                "documentId": db_features[i]["documentId"]
            })

    results = sorted(results, key=lambda x: x["similarity"], reverse=True)
    return results