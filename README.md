# DocsShare_web_backend

Các bước chạy Momo:
+ Tải ngrok(trên web)
+ Vào ngrok nhập: ngrok http 8080
+ Sẽ được 1 cái link =....free.app
+ Vào: application.properties thay link vào:momo.ipn-url và momo.return-url
+ Vào SecurityConfig: Kiếm "configuration.setAllowedOrigins" rồi thay link vào
+ Vào SwaggerConfig: Thay link cũ bằng link mới tạo được từ ngrok