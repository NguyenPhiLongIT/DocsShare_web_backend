package com.docsshare_web_backend.reasons.services;

import com.docsshare_web_backend.reasons.dto.requests.ReasonRequest;
import com.docsshare_web_backend.reasons.dto.responses.ReasonResponse;
import com.docsshare_web_backend.reasons.models.Reason;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ReasonService {
    List<ReasonResponse> getAllReasons();
    ReasonResponse getReasonById(long id);
    ReasonResponse createReason(ReasonRequest request);
    ReasonResponse updateReason(long reasonId, ReasonRequest request);
    void deleteReason(long reasonId);
}
