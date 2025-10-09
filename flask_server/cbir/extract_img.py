import fitz  # PyMuPDF
import os
from PIL import Image
import io

def extract_images_from_dir(pdf_path, output_dir, quality=85, min_size=128):
    os.makedirs(output_dir, exist_ok=True)

    pdf_doc = fitz.open(pdf_path)
    pdf_name = os.path.splitext(os.path.basename(pdf_path))[0]  

    img_counter = 1
    for page_index in range(len(pdf_doc)):
        page = pdf_doc[page_index]
        image_list = page.get_images(full=True)

        # print(f"🔍 {pdf_name} - Trang {page_index+1} có {len(image_list)} ảnh")

        for img_index, img in enumerate(image_list, start=1):
            xref = img[0]
            base_image = pdf_doc.extract_image(xref)
            width, height = base_image["width"], base_image["height"]

            if width >= min_size and height >= min_size:
                image_bytes = base_image["image"]
                image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

                image_filename = f"{pdf_name}_p{page_index+1}_i{img_index}.jpg"
                image_path = os.path.join(output_dir, image_filename)
                image.save(image_path, format="JPEG", quality=quality)

                img_counter += 1

    pdf_doc.close()
    print(f"✅ Hoàn tất {pdf_name}: {img_counter-1} ảnh đã lưu vào {output_dir}")

def extract_images_from_pdf(pdf_path, quality=85, min_size=128):
    pdf_doc = fitz.open(pdf_path)
    pdf_name = os.path.splitext(os.path.basename(pdf_path))[0]

    images = []

    for page_index in range(len(pdf_doc)):
        page = pdf_doc[page_index]
        image_list = page.get_images(full=True)
        # print(f"Trang {page_index+1} có {len(image_list)} ảnh")

        for img_index, img in enumerate(image_list, start=1):
            xref = img[0]
            base_image = pdf_doc.extract_image(xref)
            width, height = base_image["width"], base_image["height"]

            if width >= min_size and height >= min_size:
                image_bytes = base_image["image"]  # lấy raw bytes luôn
                image_filename = f"{pdf_name}_p{page_index+1}_i{img_index}.jpg"
                images.append((image_filename, image_bytes))

    pdf_doc.close()
    return images