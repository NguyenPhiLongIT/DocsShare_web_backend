package com.docsshare_web_backend.documents.services;
import org.springframework.stereotype.Service;

import com.docsshare_web_backend.documents.dto.responses.DocumentImageResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public interface DocumentImageService {
    List<DocumentImageResponse> getAllImagesWithFeatures();
    List<DocumentImageResponse> searchSimilarImages(MultipartFile file);
}
