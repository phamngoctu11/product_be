package com.example.workflow.service;

import com.example.workflow.dto.ConsultationCreateRequest;
import com.example.workflow.dto.ConsultationRequestDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.entity.Product;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.ConsultationStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.ProductRepository;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultationService {
    private static final List<ConsultationStatus> OPEN_STATUSES = List.of(
            ConsultationStatus.WAITING,
            ConsultationStatus.ASSIGNED,
            ConsultationStatus.IN_PROGRESS
    );

    private final ConsultationRequestRepository consultationRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Transactional
    public ConsultationRequestDTO createRequest(ConsultationCreateRequest request) {
        User user = getCurrentUser();
        if (user.getRole() != Role.USER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only customers can create consultation requests.");
        }

        Product product = getActiveProduct(request.getProductId());

        ConsultationRequest consultation = consultationRepository
                .findFirstByUserIdAndProductIdAndStatusInOrderByCreatedAtDesc(user.getId(), product.getId(), OPEN_STATUSES)
                .orElseGet(() -> createWaitingRequest(user, product));

        ChatMessage savedMessage = saveCustomerMessage(consultation, request.getFirstMessage());
        consultation.setLastMessageAt(savedMessage.getTimestamp());
        consultationRepository.save(consultation);

        return getDtoOrThrow(consultation.getId());
    }

    @Transactional(readOnly = true)
    public Page<ConsultationRequestDTO> getWaitingRequests(Pageable pageable) {
        return consultationRepository.findDtosByStatus(ConsultationStatus.WAITING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ConsultationRequestDTO> getMyAssignedRequests(Pageable pageable) {
        User staff = getCurrentStaff();
        return consultationRepository.findDtosByAssignedStaffIdAndStatusIn(
                staff.getId(),
                List.of(ConsultationStatus.ASSIGNED, ConsultationStatus.IN_PROGRESS),
                pageable
        );
    }

    @Transactional
    public ConsultationRequestDTO claimRequest(Long requestId) {
        User staff = getCurrentStaff();
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = consultationRepository.claimWaitingRequest(
                requestId,
                staff.getId(),
                ConsultationStatus.IN_PROGRESS.name(),
                ConsultationStatus.WAITING.name(),
                now
        );
        if (updatedRows == 0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request is no longer available.");
        }
        ConsultationRequestDTO dto = getDtoOrThrow(requestId);
        notifyCustomerStaffAccepted(dto);
        return dto;
    }

    @Transactional
    public ConsultationRequestDTO assignRequest(Long requestId, String staffId) {
        User manager = getCurrentAssigner();
        User staff = getActiveStaffById(staffId);

        int updatedRows = consultationRepository.assignWaitingRequest(
                requestId,
                staff.getId(),
                manager.getId(),
                ConsultationStatus.ASSIGNED.name(),
                ConsultationStatus.WAITING.name(),
                LocalDateTime.now()
        );
        if (updatedRows == 0) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request is no longer waiting for assignment.");
        }
        ConsultationRequestDTO dto = getDtoOrThrow(requestId);
        notifyCustomerStaffAssigned(dto);
        return dto;
    }

    @Transactional
    public ConsultationRequestDTO closeMyRequest(Long requestId) {
        User user = getCurrentUser();
        if (user.getRole() != Role.USER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only customers can close their consultation requests.");
        }

        ConsultationRequest consultation = getConsultationOrThrow(requestId);
        if (!consultation.getUser().getId().equals(user.getId())) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Customer can only close their own consultation request.");
        }

        if (OPEN_STATUSES.contains(consultation.getStatus())) {
            consultation.setStatus(ConsultationStatus.CLOSED);
            consultation.setClosedAt(LocalDateTime.now());
            consultationRepository.save(consultation);
        }
        chatService.deleteConsultationHistory(requestId);
        return getDtoOrThrow(requestId);
    }

    private ConsultationRequest createWaitingRequest(User user, Product product) {
        ConsultationRequest consultation = new ConsultationRequest();
        consultation.setUser(user);
        consultation.setProduct(product);
        consultation.setStatus(ConsultationStatus.WAITING);
        consultation.setCreatedAt(LocalDateTime.now());
        return consultationRepository.save(consultation);
    }

    private ChatMessage saveCustomerMessage(ConsultationRequest consultation, String content) {
        ChatMessage message = new ChatMessage();
        message.setConsultationRequestId(consultation.getId());
        message.setUserId(consultation.getUser().getId());
        message.setProductId(consultation.getProduct().getId());
        message.setContent(content);
        message.setShopSender(false);
        message.setMessageType("TEXT");
        return chatService.saveMessage(message);
    }

    private ConsultationRequestDTO getDtoOrThrow(Long requestId) {
        return consultationRepository.findDtoById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request not found."));
    }

    private ConsultationRequest getConsultationOrThrow(Long requestId) {
        return consultationRepository.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request not found."));
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.PRODUCT_NOT_FOUND));
        if (product.isDelete()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private User getActiveStaffById(String staffId) {
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.STAFF_NOT_FOUND, staffId));
        if (staff.getRole() != Role.STAFF || staff.isDelete()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.ACTIVE_STAFF_REQUIRED);
        }
        return staff;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }

    private User getCurrentStaff() {
        User staff = getCurrentUser();
        if (staff.getRole() != Role.STAFF) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.CURRENT_USER_STAFF_ROLE_REQUIRED);
        }
        return staff;
    }

    private void notifyCustomerStaffAccepted(ConsultationRequestDTO request) {
        notificationService.sendUserNotification(
                "Nhan vien da nhan yeu cau tu van",
                request.getAssignedStaffName() + " da nhan yeu cau tu van ve " + request.getProductName() + ".",
                request.getUserId(),
                request.getId()
        );
    }

    private void notifyCustomerStaffAssigned(ConsultationRequestDTO request) {
        notificationService.sendUserNotification(
                "Yeu cau tu van da duoc phan cong",
                request.getAssignedStaffName() + " se ho tro ban tu van ve " + request.getProductName() + ".",
                request.getUserId(),
                request.getId()
        );
    }

    private User getCurrentAssigner() {
        User manager = getCurrentUser();
        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Current user must be manager or admin to assign consultation requests.");
        }
        return manager;
    }
}
