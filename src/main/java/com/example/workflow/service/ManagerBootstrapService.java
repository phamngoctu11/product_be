package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import com.example.workflow.service.factory.UserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerBootstrapService {
    private static final int MAX_USERNAME_ATTEMPTS = 10_000;

    private final KeycloakIdentityService keycloakIdentityService;
    private final UserRepository userRepository;
    private final UserFactory userFactory;

    @Value("${keycloak.bootstrap.manager.base-username:manager}")
    private String baseUsername;

    @Value("${keycloak.bootstrap.manager.password:123456}")
    private String password;

//    public void ensureBootstrapManager() {
//        if (!StringUtils.hasText(baseUsername) || !StringUtils.hasText(password)) {
//            throw new IllegalStateException(
//                    "Cần cấu hình keycloak.bootstrap.manager.base-username và keycloak.bootstrap.manager.password"
//            );
//        }
//
//        var existing = keycloakIdentityService.findBootstrapManager(baseUsername);
//        if (existing.isPresent()) {
//            keycloakIdentityService.synchronizeBootstrapManagerRole(existing.get().id());
//            persistProfileIfMissing(existing.get());
//            log.info(
//                    "[MANAGER-BOOTSTRAP] TÀI KHOẢN ĐÃ SẴN SÀNG | username={} | role=MANAGER",
//                    existing.get().username()
//            );
//            return;
//        }
//
//        for (int suffix = 0; suffix < MAX_USERNAME_ATTEMPTS; suffix++) {
//            String username = usernameFor(suffix);
//            if (userRepository.existsByUsername(username)) {
//                continue;
//            }
//
//            UserCreDTO request = newManagerRequest(username, nextPhone(), null, null, null);
//            KeycloakIdentityService.BootstrapManagerIdentity identity;
//            try {
//                identity = keycloakIdentityService.createBootstrapManager(request);
//            } catch (AppException ex) {
//                if (ex.getErrorCode() == ConstantErrorCode.USERNAME_ALREADY_EXISTS) {
//                    var concurrentlyCreated = keycloakIdentityService.findBootstrapManager(baseUsername);
//                    if (concurrentlyCreated.isPresent()) {
//                        keycloakIdentityService.synchronizeBootstrapManagerRole(concurrentlyCreated.get().id());
//                        persistProfileIfMissing(concurrentlyCreated.get());
//                        log.info(
//                                "[MANAGER-BOOTSTRAP] TÀI KHOẢN ĐÃ SẴN SÀNG | username={} | role=MANAGER",
//                                concurrentlyCreated.get().username()
//                        );
//                        return;
//                    }
//                    continue;
//                }
//                throw ex;
//            }
//
//            try {
//                userRepository.saveAndFlush(userFactory.create(requestFrom(identity, request), identity.id(), Role.MANAGER));
//                log.info(
//                        "[MANAGER-BOOTSTRAP] TẠO TÀI KHOẢN THÀNH CÔNG | username={} | role=MANAGER | đã lưu Keycloak và MySQL",
//                        identity.username()
//                );
//                return;
//            } catch (DataIntegrityViolationException ex) {
//                keycloakIdentityService.deleteUserById(identity.id());
//                if (userRepository.existsByUsername(username)) {
//                    continue;
//                }
//                throw ex;
//            }
//        }
//
//        throw new IllegalStateException("Không tìm được username trống để tạo tài khoản manager bootstrap");
//    }

    private void persistProfileIfMissing(
            KeycloakIdentityService.BootstrapManagerIdentity identity
    ) {
        User existing = userRepository.findById(identity.id()).orElse(null);
        if (existing != null) {
            if (existing.getRole() != Role.MANAGER) {
                existing.setRole(Role.MANAGER);
                userRepository.saveAndFlush(existing);
            }
            return;
        }
        if (userRepository.existsByUsername(identity.username())) {
            throw new IllegalStateException(
                    "Username manager bootstrap đã được gắn với Keycloak ID khác trong DB: " + identity.username()
            );
        }

        UserCreDTO request = newManagerRequest(
                identity.username(), nextPhone(), identity.firstName(), identity.lastName(), identity.email()
        );
        try {
            userRepository.saveAndFlush(userFactory.create(request, identity.id(), Role.MANAGER));
        } catch (DataIntegrityViolationException ex) {
            if (!userRepository.existsById(identity.id())) {
                throw ex;
            }
        }
    }

    private UserCreDTO newManagerRequest(
            String username,
            String phone,
            String firstName,
            String lastName,
            String email
    ) {
        UserCreDTO request = new UserCreDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setFirstname(StringUtils.hasText(firstName) ? firstName : "Quản lý");
        request.setLastname(StringUtils.hasText(lastName) ? lastName : "cửa hàng");
        request.setGender("Khác");
        request.setPhone(phone);
        request.setEmail(StringUtils.hasText(email) ? email : null);
        request.setRole(Role.MANAGER.name());
        return request;
    }

    private UserCreDTO requestFrom(
            KeycloakIdentityService.BootstrapManagerIdentity identity,
            UserCreDTO fallback
    ) {
        return newManagerRequest(
                identity.username(),
                fallback.getPhone(),
                identity.firstName(),
                identity.lastName(),
                identity.email()
        );
    }

    private String usernameFor(int suffix) {
        return suffix == 0 ? baseUsername : baseUsername + suffix;
    }

    private String nextPhone() {
        for (long sequence = 1; sequence < 100_000; sequence++) {
            String phone = String.format("999%016d", sequence);
            if (!userRepository.existsByPhone(phone)) {
                return phone;
            }
        }
        throw new IllegalStateException("Không tìm được số điện thoại placeholder còn trống cho manager bootstrap");
    }
}
