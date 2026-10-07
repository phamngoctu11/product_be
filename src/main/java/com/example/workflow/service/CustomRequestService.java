package com.example.workflow.service;

import com.example.workflow.dto.CheckoutResponseDTO;
import com.example.workflow.dto.CreateCustomRequest;
import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.dto.UpdateCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderContactSnapshot;
import com.example.workflow.entity.OrderItem;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CustomRequestMapper;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.nume.OrderItemProductionStatus;
import com.example.workflow.nume.OrderItemSourceType;
import com.example.workflow.nume.OrderProductionStatus;
import com.example.workflow.nume.OrderStatus;
import com.example.workflow.nume.OrderType;
import com.example.workflow.nume.PaymentStatus;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomRequestService {
    private static final int MAX_PAGE_SIZE = 50;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CustomRequestRepository customRequestRepository;
    private final OrderRepository orderRepository;
    private final CustomRequestMapper customRequestMapper;
    private final DurableRequestExecutor durableRequests;
    private final DomainEventPublisher eventPublisher;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public CustomRequestDTO create(CreateCustomRequest request, String idempotencyKey) {
        requireCreateRequest(request);
        String ownerId = authService.getCurrentUserId();
        if (!StringUtils.hasText(idempotencyKey)) {
            return createDraft(ownerId, request);
        }

        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("spec", normalizeRequired(request.spec(), "spec"));
        canonical.put("attachments", normalizeOptional(request.attachments()));
        canonical.put("quantity", request.quantity());
        String result = durableRequests.execute(
                "custom-request:create:" + ownerId,
                idempotencyKey,
                writeJson(canonical),
                () -> writeJson(createDraft(ownerId, request))
        );
        return readCustomRequest(result);
    }

    @Transactional(readOnly = true)
    public Page<CustomRequestDTO> list(CustomRequestStatus status, Pageable pageable) {
        String ownerId = authService.getCurrentUserId();
        Pageable bounded = boundedPage(pageable);
        Page<CustomRequest> requests = status == null
                ? customRequestRepository.findAllByOwnerIdOrderByUpdatedAtDescIdDesc(ownerId, bounded)
                : customRequestRepository.findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc(ownerId, status, bounded);
        return requests.map(customRequestMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CustomRequestDTO get(Long requestId) {
        return customRequestMapper.toDto(findOwned(requestId, authService.getCurrentUserId()));
    }

    @Transactional
    public CustomRequestDTO update(Long requestId, UpdateCustomRequest request) {
        if (request == null) {
            throw invalid("Update body is required.");
        }
        String ownerId = authService.getCurrentUserId();
        CustomRequest draft = findOwned(requestId, ownerId);
        ensureEditable(draft);
        ensureVersion(draft, request.expectedVersion());
        draft.setSpec(normalizeRequired(request.spec(), "spec"));
        draft.setAttachments(normalizeOptional(request.attachments()));
        draft.setQuantity(requirePositive(request.quantity()));
        return customRequestMapper.toDto(customRequestRepository.saveAndFlush(draft));
    }

    public void delete(Long requestId, Long expectedVersion, String idempotencyKey) {
        String ownerId = authService.getCurrentUserId();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("requestId", requestId);
        canonical.put("expectedVersion", expectedVersion);
        durableRequests.execute(
                "custom-request:delete:" + ownerId + ":" + requestId,
                idempotencyKey,
                writeJson(canonical),
                () -> {
                    CustomRequest draft = lockOwned(requestId, ownerId);
                    ensureEditable(draft);
                    ensureVersion(draft, expectedVersion);
                    customRequestRepository.delete(draft);
                    customRequestRepository.flush();
                    return "{}";
                }
        );
    }

    public CheckoutResponseDTO submit(Long requestId, SubmitCustomRequest request, String idempotencyKey) {
        if (request == null) {
            throw invalid("Submit body is required.");
        }
        String ownerId = authService.getCurrentUserId();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("requestId", requestId);
        canonical.put("expectedVersion", request.expectedVersion());
        canonical.put("fullName", normalizeOptional(request.fullName()));
        canonical.put("email", normalizeEmail(request.email()));
        canonical.put("phone", normalizeOptional(request.phone()));
        canonical.put("address", normalizeOptional(request.address()));
        canonical.put("note", normalizeOptional(request.note()));

        String result = durableRequests.execute(
                "custom-request:submit:" + ownerId + ":" + requestId,
                idempotencyKey,
                writeJson(canonical),
                () -> writeJson(submitDraft(requestId, ownerId, request))
        );
        return readCheckoutResponse(result);
    }

    private CustomRequestDTO createDraft(String ownerId, CreateCustomRequest request) {
        CustomRequest draft = new CustomRequest();
        draft.setOwnerId(ownerId);
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setSpec(normalizeRequired(request.spec(), "spec"));
        draft.setAttachments(normalizeOptional(request.attachments()));
        draft.setQuantity(requirePositive(request.quantity()));
        return customRequestMapper.toDto(customRequestRepository.saveAndFlush(draft));
    }

    private CheckoutResponseDTO submitDraft(Long requestId, String ownerId, SubmitCustomRequest request) {
        CustomRequest draft = lockOwned(requestId, ownerId);
        if (!draft.isEditable()) {
            if (draft.getLinkedOrderId() != null) {
                Order existing = orderRepository.findById(draft.getLinkedOrderId())
                        .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_NOT_EDITABLE));
                return submissionResponse(existing);
            }
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_NOT_EDITABLE);
        }
        ensureVersion(draft, request.expectedVersion());
        validateForSubmission(draft);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
        OrderContactSnapshot contact = resolveContact(owner, request);
        Order order = buildCustomOrder(owner, draft, contact);
        Order saved = orderRepository.saveAndFlush(order);
        draft.markSubmitted(saved.getId(), LocalDateTime.now());
        customRequestRepository.saveAndFlush(draft);

        eventPublisher.publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(saved.getId()));
        emailService.sendCustomOrderConfirmationEmail(
                saved.getEmail(), saved.getRecipientName(), saved.getId(), draft.getSpec(), draft.getQuantity()
        );
        notificationService.sendNotification(
                "Yêu cầu custom đã được tạo",
                "Yêu cầu custom cho đơn #" + saved.getId() + " đã được tạo và đang chờ quản lý duyệt.",
                saved.getId(), ownerId, null, "/topic/user-notifications/" + ownerId
        );
        return submissionResponse(saved);
    }

    private Order buildCustomOrder(User owner, CustomRequest draft, OrderContactSnapshot contact) {
        Order order = new Order();
        order.setUser(owner);
        order.setOrderType(OrderType.CUSTOM);
        order.setStatus(OrderStatus.PENDING_APPROVAL);
        order.setPaymentStatus(PaymentStatus.NOT_DUE);
        order.setPaymentMethod(null);
        order.setPaymentMethodType(null);
        order.setProductionStatus(OrderProductionStatus.WAITING_PRODUCTION);
        order.setStartOrderTime(LocalDateTime.now());
        order.setTotalPrice(0.0);
        order.setDiscountAmount(0.0);
        order.setFinalPrice(null);
        order.setContactSnapshot(contact);
        order.setItems(new ArrayList<>());

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductVariant(null);
        item.setSourceType(OrderItemSourceType.CUSTOM);
        item.setProductNameSnapshot("Sản phẩm custom #" + draft.getId());
        item.setVariantNameSnapshot(null);
        item.setSpecSnapshot(customSnapshot(draft));
        item.setQuantity(draft.getQuantity());
        item.setPrice(null);
        item.setHandmade(true);
        item.setProductionStatus(OrderItemProductionStatus.WAITING_ASSIGNMENT);
        order.getItems().add(item);
        return order;
    }

    private OrderContactSnapshot resolveContact(User owner, SubmitCustomRequest request) {
        String fullName = firstPresent(request.fullName(), joinName(owner.getLastname(), owner.getFirstname()));
        String email = normalizeEmail(firstPresent(request.email(), owner.getEmail()));
        String phone = firstPresent(request.phone(), owner.getPhone());
        String address = firstPresent(request.address(), owner.getAddress());
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(email)
                || !StringUtils.hasText(phone) || !StringUtils.hasText(address)) {
            throw invalid("Full name, email, phone and shipping address are required when submitting.");
        }
        return new OrderContactSnapshot(
                fullName.trim(), email, phone.trim(), address.trim(), normalizeOptional(request.note())
        );
    }

    private CheckoutResponseDTO submissionResponse(Order order) {
        CheckoutResponseDTO response = new CheckoutResponseDTO();
        response.setStatus(order.getStatus().name());
        response.setMessage("Đơn custom đã được tạo và đang chờ quản lý duyệt.");
        response.setOrderId(order.getId());
        response.setVersion(order.getVersion());
        response.setPaymentStatus(order.getPaymentStatus() == null ? null : order.getPaymentStatus().name());
        response.setPaymentMethod(order.getPaymentMethodType() == null ? null : order.getPaymentMethodType().name());
        response.setTotalPrice(null);
        response.setDiscountAmount(null);
        response.setFinalPrice(null);
        return response;
    }

    private String customSnapshot(CustomRequest draft) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("spec", draft.getSpec());
        snapshot.put("attachments", draft.getAttachments());
        return writeJson(snapshot);
    }

    private void validateForSubmission(CustomRequest draft) {
        normalizeRequired(draft.getSpec(), "spec");
        requirePositive(draft.getQuantity());
    }

    private CustomRequest findOwned(Long requestId, String ownerId) {
        return customRequestRepository.findByIdAndOwnerId(requestId, ownerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CUSTOM_REQUEST_NOT_FOUND));
    }

    private CustomRequest lockOwned(Long requestId, String ownerId) {
        return customRequestRepository.findOwnedByIdForUpdate(requestId, ownerId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.CUSTOM_REQUEST_NOT_FOUND));
    }

    private void ensureEditable(CustomRequest draft) {
        if (!draft.isEditable()) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_NOT_EDITABLE);
        }
    }

    private void ensureVersion(CustomRequest draft, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(draft.getVersion())) {
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_VERSION_CONFLICT);
        }
    }

    private Pageable boundedPage(Pageable pageable) {
        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int requestedSize = pageable == null ? 20 : pageable.getPageSize();
        int size = Math.max(1, Math.min(requestedSize, MAX_PAGE_SIZE));
        return PageRequest.of(page, size);
    }

    private void requireCreateRequest(CreateCustomRequest request) {
        if (request == null) {
            throw invalid("Create body is required.");
        }
    }

    private String normalizeRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw invalid(field + " is required.");
        }
        return value.trim();
    }

    private Integer requirePositive(Integer value) {
        if (value == null || value < 1) {
            throw invalid("Quantity must be positive.");
        }
        return value;
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String firstPresent(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : normalizeOptional(fallback);
    }

    private String joinName(String lastName, String firstName) {
        return ((lastName == null ? "" : lastName.trim()) + " "
                + (firstName == null ? "" : firstName.trim())).trim();
    }

    private AppException invalid(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CUSTOM_REQUEST_INVALID, detail);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize custom request data", exception);
        }
    }

    private CustomRequestDTO readCustomRequest(String value) {
        try {
            return objectMapper.readValue(value, CustomRequestDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize custom request result", exception);
        }
    }

    private CheckoutResponseDTO readCheckoutResponse(String value) {
        try {
            return objectMapper.readValue(value, CheckoutResponseDTO.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot deserialize custom request submission", exception);
        }
    }
}
