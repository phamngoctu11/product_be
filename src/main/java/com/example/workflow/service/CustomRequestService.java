package com.example.workflow.service;

import com.example.workflow.dto.CreateCustomRequest;
import com.example.workflow.dto.CustomRequestDTO;
import com.example.workflow.dto.CustomRequestSubmissionDTO;
import com.example.workflow.dto.SubmitCustomRequest;
import com.example.workflow.dto.UpdateCustomRequest;
import com.example.workflow.entity.CustomRequest;
import com.example.workflow.entity.Order;
import com.example.workflow.entity.OrderContactSnapshot;
import com.example.workflow.entity.User;
import com.example.workflow.event.EventTypes;
import com.example.workflow.event.payload.OrderCreatedEvent;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.CustomRequestMapper;
import com.example.workflow.mapper.CustomRequestSubmissionMapper;
import com.example.workflow.nume.CustomRequestStatus;
import com.example.workflow.repository.CustomRequestRepository;
import com.example.workflow.repository.OrderRepository;
import com.example.workflow.service.consistency.DurableRequestExecutor;
import com.example.workflow.service.factory.CustomOrderFactory;
import com.example.workflow.service.redis.DomainEventPublisher;
import com.example.workflow.util.JsonUtils;
import com.example.workflow.util.PageableUtils;
import com.example.workflow.util.TextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CustomRequestService {
    private static final int MAX_DESCRIPTION_WORDS = 4_000;
    private static final Pattern WORD_PATTERN = Pattern.compile("(?U)\\S+");

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final CustomRequestRepository customRequestRepository;
    private final OrderRepository orderRepository;
    private final CustomRequestMapper customRequestMapper;
    private final CustomRequestSubmissionMapper customRequestSubmissionMapper;
    private final CustomOrderFactory customOrderFactory;
    private final DurableRequestExecutor durableRequests;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CustomRequestDTO create(CreateCustomRequest request, String idempotencyKey) {
        requireCreateRequest(request);
        String ownerId = requireActiveOwnerId();
        if (!StringUtils.hasText(idempotencyKey)) {
            return createDraft(ownerId, request);
        }

        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("description", normalizeDescription(request.description()));
        canonical.put("imageUrls", normalizeImageUrls(request.imageUrls()));
        canonical.put("quantity", request.quantity());
        String result = durableRequests.execute(
                "custom-request:create:" + ownerId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "custom request data"),
                () -> JsonUtils.write(objectMapper, createDraft(ownerId, request), "custom request data")
        );
        return JsonUtils.read(objectMapper, result, CustomRequestDTO.class, "custom request result");
    }

    @Transactional(readOnly = true)
    public Page<CustomRequestDTO> list(CustomRequestStatus status, Pageable pageable) {
        String ownerId = requireActiveOwnerId();
        Pageable bounded = boundedPage(pageable);
        Page<CustomRequest> requests = status == null
                ? customRequestRepository.findAllByOwnerIdOrderByUpdatedAtDescIdDesc(ownerId, bounded)
                : customRequestRepository.findAllByOwnerIdAndStatusOrderByUpdatedAtDescIdDesc(ownerId, status, bounded);
        return requests.map(customRequestMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CustomRequestDTO get(Long requestId) {
        return customRequestMapper.toDto(findOwned(requestId, requireActiveOwnerId()));
    }

    @Transactional
    public CustomRequestDTO update(Long requestId, UpdateCustomRequest request) {
        if (request == null) {
            throw invalid("Update body is required.");
        }
        String ownerId = requireActiveOwnerId();
        CustomRequest draft = findOwned(requestId, ownerId);
        ensureEditable(draft);
        ensureVersion(draft, request.expectedVersion());
        draft.setDescription(normalizeDescription(request.description()));
        draft.setImageUrls(normalizeImageUrls(request.imageUrls()));
        draft.setQuantity(requirePositive(request.quantity()));
        return customRequestMapper.toDto(customRequestRepository.saveAndFlush(draft));
    }

    public void delete(Long requestId, Long expectedVersion, String idempotencyKey) {
        String ownerId = requireActiveOwnerId();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("requestId", requestId);
        canonical.put("expectedVersion", expectedVersion);
        durableRequests.execute(
                "custom-request:delete:" + ownerId + ":" + requestId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "custom request data"),
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

    public CustomRequestSubmissionDTO submit(Long requestId, SubmitCustomRequest request, String idempotencyKey) {
        if (request == null) {
            throw invalid("Submit body is required.");
        }
        String ownerId = requireActiveOwnerId();
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("requestId", requestId);
        canonical.put("expectedVersion", request.expectedVersion());
        canonical.put("fullName", TextNormalizer.optional(request.fullName()));
        canonical.put("email", TextNormalizer.email(request.email()));
        canonical.put("phone", TextNormalizer.optional(request.phone()));
        canonical.put("address", TextNormalizer.optional(request.address()));
        canonical.put("note", TextNormalizer.optional(request.note()));

        String result = durableRequests.execute(
                "custom-request:submit:" + ownerId + ":" + requestId,
                idempotencyKey,
                JsonUtils.write(objectMapper, canonical, "custom request data"),
                () -> JsonUtils.write(objectMapper, submitDraft(requestId, ownerId, request), "custom request data")
        );
        return JsonUtils.read(objectMapper, result, CustomRequestSubmissionDTO.class, "custom request submission");
    }

    private CustomRequestDTO createDraft(String ownerId, CreateCustomRequest request) {
        CustomRequest draft = new CustomRequest();
        draft.setOwnerId(ownerId);
        draft.setStatus(CustomRequestStatus.DRAFT);
        draft.setDescription(normalizeDescription(request.description()));
        draft.setImageUrls(normalizeImageUrls(request.imageUrls()));
        draft.setQuantity(requirePositive(request.quantity()));
        return customRequestMapper.toDto(customRequestRepository.saveAndFlush(draft));
    }

    private CustomRequestSubmissionDTO submitDraft(Long requestId, String ownerId, SubmitCustomRequest request) {
        CustomRequest draft = lockOwned(requestId, ownerId);
        if (!draft.isEditable()) {
            if (draft.getLinkedOrderId() != null) {
                Order existing = orderRepository.findById(draft.getLinkedOrderId())
                        .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_NOT_EDITABLE));
                return submissionResponse(requestId, existing);
            }
            throw new AppException(HttpStatus.CONFLICT, ConstantErrorCode.CUSTOM_REQUEST_NOT_EDITABLE);
        }
        ensureVersion(draft, request.expectedVersion());
        validateForSubmission(draft);

        User owner = userService.requireActiveUser(ownerId);
        OrderContactSnapshot contact = resolveContact(owner, request);
        Order order = customOrderFactory.create(owner, draft, contact);
        Order saved = orderRepository.saveAndFlush(order);
        draft.markSubmitted(saved.getId(), LocalDateTime.now());
        customRequestRepository.saveAndFlush(draft);

        eventPublisher.publishAfterCommit(EventTypes.ORDER_CREATED, new OrderCreatedEvent(saved.getId()));
        return submissionResponse(requestId, saved);
    }

    private OrderContactSnapshot resolveContact(User owner, SubmitCustomRequest request) {
        String fullName = firstPresent(request.fullName(), TextNormalizer.fullName(owner.getLastname(), owner.getFirstname()));
        String email = TextNormalizer.email(firstPresent(request.email(), owner.getEmail()));
        String phone = firstPresent(request.phone(), owner.getPhone());
        String address = firstPresent(request.address(), owner.getAddress());
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(email)
                || !StringUtils.hasText(phone) || !StringUtils.hasText(address)) {
            throw invalid("Full name, email, phone and shipping address are required when submitting.");
        }
        return new OrderContactSnapshot(
                fullName.trim(), email, phone.trim(), address.trim(), TextNormalizer.optional(request.note())
        );
    }

    private CustomRequestSubmissionDTO submissionResponse(Long draftId, Order order) {
        return customRequestSubmissionMapper.toDto(draftId, order);
    }

    private void validateForSubmission(CustomRequest draft) {
        normalizeDescription(draft.getDescription());
        normalizeImageUrls(draft.getImageUrls());
        requirePositive(draft.getQuantity());
    }

    private String requireActiveOwnerId() {
        String ownerId = currentUserService.requireCurrentUserId();
        userService.requireActiveUser(ownerId);
        return ownerId;
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
        return PageableUtils.normalize(pageable, 20, 50);
    }

    private void requireCreateRequest(CreateCustomRequest request) {
        if (request == null) {
            throw invalid("Create body is required.");
        }
    }

    private String normalizeDescription(String value) {
        if (!StringUtils.hasText(value)) {
            throw invalid("Description is required.");
        }
        String normalized = value.trim();
        long wordCount = WORD_PATTERN.matcher(normalized).results().count();
        if (wordCount > MAX_DESCRIPTION_WORDS) {
            throw invalid("Description must not exceed " + MAX_DESCRIPTION_WORDS + " words.");
        }
        return normalized;
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            throw invalid("Image URLs are required; use an empty list when there are no images.");
        }
        List<String> normalized = new ArrayList<>(imageUrls.size());
        for (String value : imageUrls) {
            if (!StringUtils.hasText(value)) {
                throw invalid("Image URL must not be blank.");
            }
            String url = value.trim();
            try {
                URI uri = new URI(url);
                String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
                if (!("http".equals(scheme) || "https".equals(scheme)) || !StringUtils.hasText(uri.getHost())) {
                    throw invalid("Each image URL must be an absolute HTTP or HTTPS URL.");
                }
            } catch (URISyntaxException exception) {
                throw invalid("Each image URL must be an absolute HTTP or HTTPS URL.");
            }
            normalized.add(url);
        }
        return List.copyOf(normalized);
    }

    private Integer requirePositive(Integer value) {
        if (value == null || value < 1) {
            throw invalid("Quantity must be positive.");
        }
        return value;
    }

    private String firstPresent(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred.trim() : TextNormalizer.optional(fallback);
    }

    private AppException invalid(String detail) {
        return new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.CUSTOM_REQUEST_INVALID, detail);
    }

}
