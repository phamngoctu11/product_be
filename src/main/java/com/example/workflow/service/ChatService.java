package com.example.workflow.service;

import com.example.workflow.dto.ChatUserDTO;
import com.example.workflow.dto.ConsultationRequestDTO;
import com.example.workflow.entity.ChatMessage;
import com.example.workflow.entity.ConsultationRequest;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.ConsultationStatus;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.ChatMessageRepository;
import com.example.workflow.repository.ConsultationRequestRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.redis.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final List<ConsultationStatus> OPEN_STATUSES = List.of(
            ConsultationStatus.WAITING,
            ConsultationStatus.ASSIGNED,
            ConsultationStatus.IN_PROGRESS
    );
    private static final List<ConsultationStatus> STAFF_CHAT_STATUSES = List.of(
            ConsultationStatus.ASSIGNED,
            ConsultationStatus.IN_PROGRESS
    );

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ConsultationRequestRepository consultationRepository;
    private final MongoTemplate mongoTemplate;
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(String userId) {
        User currentUser = getCurrentUser();
        validateUserChatReadAccess(userId, currentUser);
        return chatMessageRepository.findByUserIdAndConsultationRequestIdIsNullOrderByTimestampAsc(userId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getConsultationChatHistory(Long consultationRequestId, Long productId) {
        ConsultationRequest consultation = getConsultationOrThrow(consultationRequestId);
        User currentUser = getCurrentUser();
        validateConsultationReadAccess(consultation, currentUser);
        if (productId != null) {
            if (!consultation.getProduct().getId().equals(productId)) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Product does not belong to this consultation request.");
            }
            return chatMessageRepository.findByConsultationRequestIdAndProductIdOrderByTimestampAsc(consultationRequestId, productId);
        }
        return chatMessageRepository.findByConsultationRequestIdOrderByTimestampAsc(consultationRequestId);
    }

    @Transactional(readOnly = true)
    public List<ChatUserDTO> getChattedUsers() {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.STAFF) {
            return getAssignedChatUsers(currentUser);
        }
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
            return getAllChattedUsers();
        }
        return getCustomerChatThreads(currentUser);
    }

    private List<ChatUserDTO> getAllChattedUsers() {
        List<ConsultationRequestDTO> requests = consultationRepository
                .findDtosByStatusIn(OPEN_STATUSES, Pageable.unpaged())
                .getContent();

        List<String> userIds = mongoTemplate.query(ChatMessage.class)
                .distinct("userId")
                .as(String.class)
                .all();

        List<String> requestUserIds = requests.stream()
                .map(ConsultationRequestDTO::getUserId)
                .distinct()
                .toList();
        List<String> allUserIds = Stream.concat(userIds.stream(), requestUserIds.stream())
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (allUserIds.isEmpty()) {
            return List.of();
        }

        Map<String, ChatUserDTO> usersById = loadChatUsersById(allUserIds);

        List<ChatUserDTO> chatUsers = new ArrayList<>();
        for (ConsultationRequestDTO request : requests) {
            ChatUserDTO enrichedUser = enrichChatUser(usersById.get(request.getUserId()), request);
            if (enrichedUser != null) {
                chatUsers.add(enrichedUser);
            }
        }

        Map<String, ChatUserDTO> consultationUsersById = chatUsers.stream()
                .collect(Collectors.toMap(ChatUserDTO::getId, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        userIds.stream()
                .filter(userId -> !consultationUsersById.containsKey(userId))
                .map(usersById::get)
                .filter(user -> user != null)
                .forEach(chatUsers::add);

        return chatUsers;
    }

    private List<ChatUserDTO> getAssignedChatUsers(User staff) {
        List<ConsultationRequestDTO> requests = consultationRepository
                .findDtosByAssignedStaffIdAndStatusIn(staff.getId(), STAFF_CHAT_STATUSES, Pageable.unpaged())
                .getContent();
        if (requests.isEmpty()) {
            return List.of();
        }

        List<String> userIds = requests.stream()
                .map(ConsultationRequestDTO::getUserId)
                .distinct()
                .toList();
        Map<String, ChatUserDTO> usersById = loadChatUsersById(userIds);

        return requests.stream()
                .map(request -> enrichChatUser(usersById.get(request.getUserId()), request))
                .filter(chatUser -> chatUser != null)
                .toList();
    }

    private List<ChatUserDTO> getCustomerChatThreads(User user) {
        List<ConsultationRequestDTO> requests = consultationRepository
                .findDtosByUserIdAndStatusIn(user.getId(), OPEN_STATUSES, Pageable.unpaged())
                .getContent();
        if (requests.isEmpty()) {
            return List.of();
        }

        return requests.stream()
                .map(request -> toCustomerThread(user, request))
                .toList();
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessage chatMessage) {
        validateBasicMessage(chatMessage);

        User sender = resolveSender(chatMessage);
        ConsultationRequest consultation = resolveConsultation(chatMessage, sender);
        validateMessageWriteAccess(chatMessage, sender, consultation);
        boolean notifyCustomer = shouldNotifyCustomerOnFirstStaffReply(sender, consultation);

        LocalDateTime now = LocalDateTime.now();
        chatMessage.setId(null);
        chatMessage.setContent(chatMessage.getContent().trim());
        chatMessage.setTimestamp(now);
        applySenderMetadata(chatMessage, sender);
        applyConsultationContext(chatMessage, consultation);
        updateConsultationAfterMessage(consultation, sender, now);

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        if (notifyCustomer) {
            notifyCustomerWhenStaffReplies(sender, consultation);
        }
        return savedMessage;
    }

    public long deleteConsultationHistory(Long consultationRequestId) {
        return chatMessageRepository.deleteByConsultationRequestId(consultationRequestId);
    }

    private void validateBasicMessage(ChatMessage chatMessage) {
        boolean hasValidUserId = chatMessage.getUserId() != null ;
        boolean hasValidConsultationId = chatMessage.getConsultationRequestId() != null && chatMessage.getConsultationRequestId() > 0;
        if (!hasValidUserId && !hasValidConsultationId) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Valid userId or consultationRequestId is required.");
        }
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "Message content is required.");
        }
        if (chatMessage.getMessageType() == null || chatMessage.getMessageType().isBlank()) {
            chatMessage.setMessageType("TEXT");
        }
    }

    private User resolveSender(ChatMessage chatMessage) {
        User authenticatedUser = getCurrentAuthenticatedUserOrNull();
        if (authenticatedUser != null) {
            return authenticatedUser;
        }

        if (chatMessage.getSenderId() != null) {
            return getActiveUserById(chatMessage.getSenderId());
        }

        if (!chatMessage.isShopSender() && chatMessage.getUserId() != null) {
            return getActiveUserById(chatMessage.getUserId());
        }

        throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Sender identity is required to send consultation messages.");
    }

    private ConsultationRequest resolveConsultation(ChatMessage chatMessage, User sender) {
        if (chatMessage.getConsultationRequestId() != null) {
            return getConsultationOrThrow(chatMessage.getConsultationRequestId());
        }

        if (sender.getRole() == Role.STAFF) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "consultationRequestId is required for staff messages.");
        }

        return null;
    }

    private void validateMessageWriteAccess(ChatMessage chatMessage, User sender, ConsultationRequest consultation) {
        if (sender.getRole() == Role.ADMIN || sender.getRole() == Role.MANAGER) {
            throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Admin and manager can only view or assign consultation requests.");
        }

        if (sender.getRole() == Role.USER) {
            if (chatMessage.isShopSender()) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Customer cannot send shop messages.");
            }
            if (chatMessage.getUserId() != null && !sender.getId().equals(chatMessage.getUserId())) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Customer can only send messages in their own chat.");
            }
            if (consultation != null && !consultation.getUser().getId().equals(sender.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Customer can only send messages in their own consultation request.");
            }
            validateOpenConsultation(consultation);
            return;
        }

        if (sender.getRole() == Role.STAFF) {
            if (consultation == null) {
                throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.BAD_REQUEST_DETAIL, "consultationRequestId is required for staff messages.");
            }
            if (consultation.getAssignedStaff() == null || !consultation.getAssignedStaff().getId().equals(sender.getId())) {
                throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "Only assigned staff can reply to this consultation request.");
            }
            if (!STAFF_CHAT_STATUSES.contains(consultation.getStatus())) {
                throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request is not active for staff reply.");
            }
            return;
        }

        throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "User role is not allowed to send chat messages.");
    }

    private void validateOpenConsultation(ConsultationRequest consultation) {
        if (consultation != null && !OPEN_STATUSES.contains(consultation.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request is no longer active.");
        }
    }

    private void validateUserChatReadAccess(String userId, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
            return;
        }
        if (currentUser.getRole() == Role.USER && currentUser.getId().equals(userId)) {
            return;
        }
        if (currentUser.getRole() == Role.STAFF && consultationRepository.existsByUserIdAndAssignedStaffIdAndStatusIn(
                userId,
                currentUser.getId(),
                STAFF_CHAT_STATUSES
        )) {
            return;
        }

        throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "You do not have access to this chat.");
    }

    private void validateConsultationReadAccess(ConsultationRequest consultation, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
            return;
        }
        if (currentUser.getRole() == Role.USER && consultation.getUser().getId().equals(currentUser.getId())) {
            return;
        }
        if (currentUser.getRole() == Role.STAFF
                && consultation.getAssignedStaff() != null
                && consultation.getAssignedStaff().getId().equals(currentUser.getId())) {
            return;
        }

        throw new AppException(HttpStatus.FORBIDDEN, ConstantErrorCode.BAD_REQUEST_DETAIL, "You do not have access to this consultation chat.");
    }

    private void applySenderMetadata(ChatMessage chatMessage, User sender) {
        chatMessage.setSenderId(sender.getId());
        chatMessage.setSenderRole(sender.getRole().name());
        chatMessage.setSenderName(buildFullName(sender));
        chatMessage.setShopSender(sender.getRole() == Role.STAFF);
    }

    private void applyConsultationContext(ChatMessage chatMessage, ConsultationRequest consultation) {
        if (consultation == null) {
            return;
        }

        chatMessage.setConsultationRequestId(consultation.getId());
        chatMessage.setUserId(consultation.getUser().getId());
        chatMessage.setProductId(consultation.getProduct().getId());
        if (consultation.getAssignedStaff() != null) {
            chatMessage.setAssignedStaffId(consultation.getAssignedStaff().getId());
            chatMessage.setAssignedStaffName(buildFullName(consultation.getAssignedStaff()));
        }
    }

    private void updateConsultationAfterMessage(ConsultationRequest consultation, User sender, LocalDateTime timestamp) {
        if (consultation == null) {
            return;
        }

        consultation.setLastMessageAt(timestamp);
        if (sender.getRole() == Role.STAFF) {
            if (consultation.getStatus() == ConsultationStatus.ASSIGNED) {
                consultation.setStatus(ConsultationStatus.IN_PROGRESS);
            }
            if (consultation.getFirstStaffReplyAt() == null) {
                consultation.setFirstStaffReplyAt(timestamp);
            }
            if (consultation.getClaimedAt() == null) {
                consultation.setClaimedAt(timestamp);
            }
        }
        consultationRepository.save(consultation);
    }

    private void notifyCustomerWhenStaffReplies(User sender, ConsultationRequest consultation) {
        if (consultation == null) {
            return;
        }

        String productName = consultation.getProduct() == null ? "san pham" : consultation.getProduct().getProductName();
        notificationService.sendUserNotification(
                "Nhan vien da phan hoi tu van",
                buildFullName(sender) + " da phan hoi yeu cau tu van ve " + productName + ".",
                consultation.getUser().getId(),
                consultation.getId()
        );
    }

    private boolean shouldNotifyCustomerOnFirstStaffReply(User sender, ConsultationRequest consultation) {
        return sender.getRole() == Role.STAFF
                && consultation != null
                && !chatMessageRepository.existsByConsultationRequestIdAndSenderRole(
                        consultation.getId(),
                        Role.STAFF.name()
                );
    }

    private ChatUserDTO enrichChatUser(ChatUserDTO source, ConsultationRequestDTO request) {
        if (source == null) {
            return null;
        }

        ChatUserDTO chatUser = new ChatUserDTO(
                source.getId(),
                source.getFirstname(),
                source.getLastname(),
                source.getEmail(),
                source.getAvatarUrl(),
                source.getIsActive()
        );
        applyConsultationThreadMetadata(chatUser, request);
        chatUser.setChatTitle(request.getCustomerName() + " - " + request.getProductName());
        return chatUser;
    }

    private ChatUserDTO toCustomerThread(User user, ConsultationRequestDTO request) {
        ChatUserDTO chatUser = toChatUserDTO(user);
        applyConsultationThreadMetadata(chatUser, request);
        chatUser.setIsActive(request.getAssignedStaffId() != null && chatPresenceService.isOnline(request.getAssignedStaffId()));

        String staffName = request.getAssignedStaffName() == null || request.getAssignedStaffName().isBlank()
                ? "Dang cho nhan vien"
                : request.getAssignedStaffName();
        chatUser.setChatTitle(request.getProductName() + " - " + staffName);
        return chatUser;
    }

    private Map<String, ChatUserDTO> loadChatUsersById(List<String> userIds) {
        return userRepository.findChatUserDtosByIds(userIds)
                .stream()
                .peek(user -> user.setIsActive(chatPresenceService.isOnline(user.getId())))
                .collect(Collectors.toMap(ChatUserDTO::getId, Function.identity(), (first, second) -> first));
    }

    private void applyConsultationThreadMetadata(ChatUserDTO chatUser, ConsultationRequestDTO request) {
        chatUser.setChatThreadId(request.getId());
        chatUser.setConsultationRequestId(request.getId());
        chatUser.setProductId(request.getProductId());
        chatUser.setProductName(request.getProductName());
        chatUser.setProductImageUrl(request.getProductImageUrl());
        chatUser.setAssignedStaffId(request.getAssignedStaffId());
        chatUser.setAssignedStaffName(request.getAssignedStaffName());
        chatUser.setAssignedByManagerId(request.getAssignedByManagerId());
        chatUser.setAssignedByManagerName(request.getAssignedByManagerName());
    }

    private ChatUserDTO toChatUserDTO(User user) {
        return new ChatUserDTO(
                user.getId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getAvatarUrl(),
                false
        );
    }

    private User getCurrentUser() {
        User user = getCurrentAuthenticatedUserOrNull();
        if (user == null) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.BAD_REQUEST_DETAIL, "Authentication is required.");
        }
        return user;
    }

    private ConsultationRequest getConsultationOrThrow(Long consultationRequestId) {
        return consultationRepository.findById(consultationRequestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.BAD_REQUEST_DETAIL, "Consultation request not found."));
    }

    private User getCurrentAuthenticatedUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private User getActiveUserById(String userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDelete())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND_WITH_ID, userId));
    }

    private String buildFullName(User user) {
        String fullName = Stream.of(user.getLastname(), user.getFirstname())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return user.getUsername();
    }
}
