package com.docsshare_web_backend.reasons.services.impl;

import com.docsshare_web_backend.reasons.dto.requests.ReasonRequest;
import com.docsshare_web_backend.reasons.dto.responses.ReasonResponse;
import com.docsshare_web_backend.reasons.models.Reason;
import com.docsshare_web_backend.reasons.repositories.ReasonRepository;
import com.docsshare_web_backend.reasons.services.ReasonService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReasonServiceImpl implements ReasonService {
    @Autowired
    private ReasonRepository reasonRepository;

    public static class ReasonMapper{
        public static ReasonResponse toReasonResponse(Reason reason){
            return ReasonResponse.builder()
                    .id(reason.getId())
                    .name(reason.getName())
                    .description(reason.getDescription())
                    .build();
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<ReasonResponse> getAllReasons() {
        return reasonRepository.findAll()
                .stream()
                .map(ReasonMapper::toReasonResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReasonResponse getReasonById(long id) {
        Reason reason = reasonRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reason not found with id: " + id));
        return ReasonMapper.toReasonResponse(reason);
    }

    @Override
    @Transactional
    public ReasonResponse createReason(ReasonRequest request) {
        if (reasonRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Lý do đã tồn tại với nội dung: " + request.getName());
        }
        Reason reason = Reason.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        Reason saved = reasonRepository.save(reason);
        return ReasonMapper.toReasonResponse(saved);
    }

    @Override
    @Transactional
    public ReasonResponse updateReason(long reasonId, ReasonRequest request) {
        Reason reason = reasonRepository.findById(reasonId)
                .orElseThrow(() -> new EntityNotFoundException("Reason not found with id: " + reasonId));
        if (reasonRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Lý do đã tồn tại với nội dung: " + request.getName());
        }
        reason.setName(request.getName());
        reason.setDescription(request.getDescription());

        Reason updated = reasonRepository.save(reason);
        return ReasonMapper.toReasonResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReason(long reasonId) {
        if (!reasonRepository.existsById(reasonId)) {
            throw new EntityNotFoundException("Reason not found with id: " + reasonId);
        }
        reasonRepository.deleteById(reasonId);
    }
}
