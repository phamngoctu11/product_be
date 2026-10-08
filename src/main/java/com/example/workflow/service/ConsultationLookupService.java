package com.example.workflow.service;

import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.repository.ConsultationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsultationLookupService {

    private final ConsultationRequestRepository consultationRepository;

    @Transactional(readOnly = true)
    public ConsultationRequest require(Long requestId) {
        return consultationRepository.findById(requestId)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        ConstantErrorCode.BAD_REQUEST_DETAIL,
                        "Consultation request not found."
                ));
    }
}
