package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KeycloakIdentityService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.admin.realm}")
    private String adminRealm;
    @Value("${keycloak.admin.client-id}")
    private String adminClientId;
    @Value("${keycloak.admin.username}")
    private String adminUsername;
    @Value("${keycloak.admin.password}")
    private String adminPassword;

    public String authenticate(String username, String password) {
        try {
            Object accessToken = requestToken(realm, clientId, username, password).get("access_token");
            if (accessToken instanceof String token && !token.isBlank()) {
                return token;
            }
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS);
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_UNAVAILABLE);
        }
    }

    public String createUser(UserCreDTO user, Role role) {
        return createUser(user, role, Map.of());
    }

    public BootstrapManagerIdentity createBootstrapManager(UserCreDTO user) {
        String userId = createUser(
                user,
                Role.MANAGER,
                Map.of("bootstrap_manager", List.of("true"))
        );
        try {
            return getBootstrapManagerIdentity(userId);
        } catch (RuntimeException ex) {
            deleteUserById(userId);
            throw ex;
        }
    }

    public Optional<BootstrapManagerIdentity> findBootstrapManager(String usernamePrefix) {
        String adminToken = getAdminToken();
        String encodedPrefix = URLEncoder.encode(usernamePrefix, StandardCharsets.UTF_8);
        List<Map<String, Object>> users = exchangeAdmin(
                adminUrl("/users?search=" + encodedPrefix + "&max=1000"),
                HttpMethod.GET,
                adminToken,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        ).getBody();
        if (users == null) {
            return Optional.empty();
        }
        return users.stream()
                .filter(this::isBootstrapManager)
                .filter(user -> stringValue(user.get("username")).startsWith(usernamePrefix))
                .findFirst()
                .map(this::toBootstrapManagerIdentity);
    }

    public void synchronizeBootstrapManagerRole(String userId) {
        synchronizeRealmRole(getAdminToken(), userId, Role.MANAGER);
    }

    private String createUser(UserCreDTO user, Role role, Map<String, List<String>> attributes) {
        String adminToken = getAdminToken();
        Map<String, Object> representation = new HashMap<>();
        representation.put("username", user.getUsername());
        representation.put("enabled", true);
        representation.put("firstName", user.getFirstname());
        representation.put("lastName", user.getLastname());
        representation.put("email", user.getEmail() == null ? "" : user.getEmail());
        representation.put("credentials", List.of(Map.of(
                "type", "password",
                "value", user.getPassword(),
                "temporary", false
        )));
        if (!attributes.isEmpty()) {
            representation.put("attributes", attributes);
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    adminUrl("/users"), HttpMethod.POST,
                    adminEntity(adminToken, representation), Void.class
            );
            String userId = extractCreatedId(response.getHeaders().getLocation());
            try {
                assignRealmRole(adminToken, userId, role.name());
            } catch (RuntimeException ex) {
                deleteUserById(userId);
                throw ex;
            }
            return userId;
        } catch (HttpClientErrorException.Conflict ex) {
            throw new AppException(HttpStatus.BAD_REQUEST, ConstantErrorCode.USERNAME_ALREADY_EXISTS);
        } catch (AppException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
    }

    private BootstrapManagerIdentity getBootstrapManagerIdentity(String userId) {
        Map<String, Object> user = exchangeAdmin(
                adminUrl("/users/" + userId),
                HttpMethod.GET,
                getAdminToken(),
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        ).getBody();
        if (user == null || stringValue(user.get("id")).isBlank()) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
        return toBootstrapManagerIdentity(user);
    }

    private boolean isBootstrapManager(Map<String, Object> user) {
        Object rawAttributes = user.get("attributes");
        if (!(rawAttributes instanceof Map<?, ?> attributes)) {
            return false;
        }
        Object marker = attributes.get("bootstrap_manager");
        if (marker instanceof List<?> values) {
            return values.stream().anyMatch(value -> "true".equalsIgnoreCase(String.valueOf(value)));
        }
        return "true".equalsIgnoreCase(String.valueOf(marker));
    }

    private BootstrapManagerIdentity toBootstrapManagerIdentity(Map<String, Object> user) {
        return new BootstrapManagerIdentity(
                stringValue(user.get("id")),
                stringValue(user.get("username")),
                stringValue(user.get("firstName")),
                stringValue(user.get("lastName")),
                stringValue(user.get("email"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public record BootstrapManagerIdentity(
            String id,
            String username,
            String firstName,
            String lastName,
            String email
    ) {
    }

    public void updateUser(String userId, UserCreDTO user, Role role) {
        String adminToken = getAdminToken();
        updateProfile(adminToken, userId, user.getUsername(), user.getFirstname(), user.getLastname(), user.getEmail());
        synchronizeRealmRole(adminToken, userId, role);
    }

    public void updateUserProfile(String userId, String username, UserProfileUpdateDTO user) {
        String adminToken = getAdminToken();
        updateProfile(adminToken, userId, username, user.getFirstname(), user.getLastname(), user.getEmail());
    }

    public void resetPassword(String userId, String newPassword, boolean temporary) {
        String adminToken = getAdminToken();
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", temporary
        );
        exchangeAdmin(adminUrl("/users/" + userId + "/reset-password"), HttpMethod.PUT, adminToken, credential, Void.class);
    }

    private void updateProfile(
            String adminToken,
            String userId,
            String username,
            String firstname,
            String lastname,
            String email
    ) {
        Map<String, Object> representation = new HashMap<>();
        representation.put("username", username);
        representation.put("firstName", firstname);
        representation.put("lastName", lastname);
        representation.put("email", email == null ? "" : email);
        exchangeAdmin(adminUrl("/users/" + userId), HttpMethod.PUT, adminToken, representation, Void.class);
    }

    public void disableUser(String userId) {
        String adminToken = getAdminToken();
        exchangeAdmin(adminUrl("/users/" + userId), HttpMethod.PUT, adminToken, Map.of("enabled", false), Void.class);
    }

    public void deleteUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            String adminToken = getAdminToken();
            exchangeAdmin(adminUrl("/users/" + userId), HttpMethod.DELETE, adminToken, null, Void.class);
        } catch (RuntimeException ignored) {
            // Best-effort rollback when the local database write fails.
        }
    }

    private void assignRealmRole(String adminToken, String userId, String roleName) {
        Map<String, Object> role = exchangeAdmin(
                adminUrl("/roles/" + roleName), HttpMethod.GET, adminToken, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                }
        ).getBody();
        if (role == null) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
        exchangeAdmin(
                adminUrl("/users/" + userId + "/role-mappings/realm"),
                HttpMethod.POST, adminToken, List.of(role), Void.class
        );
    }

    private void synchronizeRealmRole(String adminToken, String userId, Role desiredRole) {
        if (desiredRole == null) {
            return;
        }

        List<Map<String, Object>> assignedRoles = exchangeAdmin(
                adminUrl("/users/" + userId + "/role-mappings/realm"),
                HttpMethod.GET,
                adminToken,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        ).getBody();

        List<Map<String, Object>> applicationRoles = new ArrayList<>();
        if (assignedRoles != null) {
            for (Map<String, Object> assignedRole : assignedRoles) {
                Object roleName = assignedRole.get("name");
                if (roleName instanceof String name && isApplicationRole(name)) {
                    applicationRoles.add(assignedRole);
                }
            }
        }

        boolean alreadyAssigned = applicationRoles.stream()
                .anyMatch(role -> desiredRole.name().equalsIgnoreCase(String.valueOf(role.get("name"))));
        if (alreadyAssigned && applicationRoles.size() == 1) {
            return;
        }

        if (!applicationRoles.isEmpty()) {
            exchangeAdmin(
                    adminUrl("/users/" + userId + "/role-mappings/realm"),
                    HttpMethod.DELETE,
                    adminToken,
                    applicationRoles,
                    Void.class
            );
        }
        assignRealmRole(adminToken, userId, desiredRole.name());
    }

    private boolean isApplicationRole(String roleName) {
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(roleName)) {
                return true;
            }
        }
        return false;
    }

    private String getAdminToken() {
        try {
            Object token = requestToken(adminRealm, adminClientId, adminUsername, adminPassword).get("access_token");
            if (token instanceof String value && !value.isBlank()) {
                return value;
            }
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_UNAVAILABLE);
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requestToken(String tokenRealm, String tokenClientId, String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", tokenClientId);
        form.add("username", username);
        form.add("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                serverUrl + "/realms/" + tokenRealm + "/protocol/openid-connect/token",
                new HttpEntity<>(form, headers), Map.class
        );
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private <T> ResponseEntity<T> exchangeAdmin(
            String url, HttpMethod method, String token, Object body, Class<T> responseType
    ) {
        try {
            return restTemplate.exchange(url, method, adminEntity(token, body), responseType);
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
    }

    private <T> ResponseEntity<T> exchangeAdmin(
            String url, HttpMethod method, String token, Object body, ParameterizedTypeReference<T> responseType
    ) {
        try {
            return restTemplate.exchange(url, method, adminEntity(token, body), responseType);
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
    }

    private HttpEntity<Object> adminEntity(String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String adminUrl(String path) {
        return serverUrl + "/admin/realms/" + realm + path;
    }

    private String extractCreatedId(URI location) {
        if (location == null || location.getPath() == null) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, ConstantErrorCode.KEYCLOAK_USER_SYNC_FAILED);
        }
        String path = location.getPath();
        // Đã sửa: Xóa bỏ dấu gạch chéo ở cuối chuỗi nếu có để tránh việc cắt ra chuỗi rỗng
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
