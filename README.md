# 📚 DocsShare Backend API - Hệ Thống Chia Sẻ & Kết Nối Tri Thức Trực Tuyến Tích Hợp AI

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-00000F?style=for-the-badge&logo=mysql&logoColor=white)
![Python](https://img.shields.io/badge/Python-14354C?style=for-the-badge&logo=python&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)

Đây là mã nguồn Backend (Core API & AI Services) cho đồ án tốt nghiệp **"Xây dựng hệ thống chia sẻ và kết nối tri thức trực tuyến với chức năng tìm kiếm tài liệu dựa trên kỹ thuật AI"**. Dự án được thực hiện bởi sinh viên Học viện Công nghệ Bưu chính Viễn thông (PTIT) cơ sở TP.HCM.

## 📖 Giới thiệu dự án

**DocsShare** là một nền tảng trực tuyến toàn diện cho phép người dùng lưu trữ, chia sẻ, thảo luận và kinh doanh tài liệu số. Backend của hệ thống được thiết kế theo mô hình Client-Server, đảm nhiệm xử lý logic nghiệp vụ phức tạp, quản lý cơ sở dữ liệu và đặc biệt là giao tiếp với các dịch vụ Microservice AI (Python Flask) để cung cấp các tính năng thông minh.

## 🚀 Các tính năng chính (Backend Modules)

* **Quản lý Tài liệu số:** API hỗ trợ upload đa định dạng (PDF, DOCX, JPG/PNG, MP4, MP3), lưu trữ an toàn qua Google Drive API, quản lý quyền truy cập (Public/Private).
* **Tìm kiếm thông minh (AI-Powered Search):**
    * **Tìm kiếm theo ngữ nghĩa (Semantic Search):** Tích hợp mô hình `multilingual-e5-base` giúp tìm kiếm tài liệu dựa trên ý nghĩa câu truy vấn thay vì chỉ so khớp từ khóa.
    * **Tìm kiếm bằng hình ảnh (CBIR):** Tích hợp mạng học sâu *Convolutional Autoencoder* để truy xuất tài liệu có hình ảnh tương đồng.
* **Tóm tắt tự động:** Gọi API sang service AI sử dụng mô hình ngôn ngữ lớn **PhoBERT** để tự động sinh tóm tắt nội dung khi tài liệu được tải lên.
* **Kiểm duyệt nội dung tự động:** Tích hợp mô hình *XLM-R* (tiếng Việt) và *Logistic Regression + TF-IDF* (tiếng Anh) để tự động phát hiện và chặn các bình luận/bài đăng độc hại trên diễn đàn.
* **Thương mại điện tử (E-commerce):** Quản lý giỏ hàng, đơn hàng, bảo vệ bản quyền tài liệu trả phí và xử lý thanh toán (tích hợp cổng thanh toán MoMo).
* **Quản lý người dùng & Phân quyền:** Xác thực và cấp phép an toàn với Spring Security & JWT cho các vai trò: `USER`, `STAFF`, `ADMIN`.

## 🛠 Công nghệ sử dụng

* **Ngôn ngữ lập trình:** Java, Python.
* **Framework chính:** Spring Boot (RESTful API, Spring Data JPA, Spring Security).
* **Cơ sở dữ liệu:** MySQL (InnoDB).
* **AI & Machine Learning:**
    * **Framework:** Python Flask (Microservice xử lý mô hình AI), PyTorch/TensorFlow, Transformers, FAISS.
    * **Mô hình:** PhoBERT, XLM-R, Convolutional Autoencoder, multilingual-e5-base.
* **Lưu trữ đám mây:** Google Drive API.

## 👨‍💻 Thông tin Tác giả

* **Nguyễn Phi Long** (N21DCCN142) - Phát triển Backend API, module quản lý tài liệu, quản lý người dùng & Xây dựng mô hình AI (CBIR, Kiểm duyệt nội dung).
* **Nguyễn Văn Đại** (N20DCCN093) - Phát triển module Diễn đàn, Kinh doanh tài liệu số & Xây dựng mô hình AI (Tóm tắt tài liệu, Semantic Search).
* **Giảng viên hướng dẫn:** ThS. Nguyễn Ngọc Duy.
* **Đơn vị:** Khoa Công nghệ Thông tin - Học viện Công nghệ Bưu chính Viễn thông (PTIT) cơ sở TP.HCM.

---
*Dự án được hoàn thiện vào năm 2025 phục vụ cho quá trình đánh giá Đồ án Tốt nghiệp Đại học.*
