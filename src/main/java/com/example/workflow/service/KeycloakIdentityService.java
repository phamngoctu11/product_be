package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
        String adminToken = getAdminToken();
        Map<String, Object> representation = Map.of(
                "username", user.getUsername(),
                "enabled", true,
                "firstName", user.getFirstname(),
                "lastName", user.getLastname(),
                "email", user.getEmail() == null ? "" : user.getEmail(),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", user.getPassword(),
                        "temporary", false
                ))
        );

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

    public void updateUser(String username, UserCreDTO user) {
        String adminToken = getAdminToken();
        String userId = findUserId(adminToken, username);
        Map<String, Object> representation = Map.of(
                "firstName", user.getFirstname(),
                "lastName", user.getLastname(),
                "email", user.getEmail() == null ? "" : user.getEmail()
        );
        exchangeAdmin(adminUrl("/users/" + userId), HttpMethod.PUT, adminToken, representation, Void.class);
    }

    public void disableUser(String username) {
        String adminToken = getAdminToken();
        String userId = findUserId(adminToken, username);
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

    private String findUserId(String adminToken, String username) {
        URI uri = UriComponentsBuilder.fromHttpUrl(adminUrl("/users"))
                .queryParam("username", username)
                .queryParam("exact", true)
                .build().encode().toUri();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                uri, HttpMethod.GET, adminEntity(adminToken, null),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        );
        List<Map<String, Object>> users = response.getBody();
        if (users == null || users.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.KEYCLOAK_USER_NOT_FOUND);
        }
        return String.valueOf(users.get(0).get("id"));
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