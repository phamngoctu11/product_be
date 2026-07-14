package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.nume.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakIdentityServiceTest {
    private KeycloakIdentityService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new KeycloakIdentityService();
        ReflectionTestUtils.setField(service, "serverUrl", "http://localhost:8180");
        ReflectionTestUtils.setField(service, "realm", "my-workflow-dev");
        ReflectionTestUtils.setField(service, "clientId", "workflow-frontend");
        ReflectionTestUtils.setField(service, "adminRealm", "master");
        ReflectionTestUtils.setField(service, "adminClientId", "admin-cli");
        ReflectionTestUtils.setField(service, "adminUsername", "admin");
        ReflectionTestUtils.setField(service, "adminPassword", "admin-password");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void updateUserSynchronizesProfileAndApplicationRealmRoleByKeycloakId() {
        String adminBase = "http://localhost:8180/admin/realms/my-workflow-dev";
        server.expect(requestTo("http://localhost:8180/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(adminBase + "/users/keycloak-user-id"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {
                          "username": "updated-user",
                          "firstName": "Updated",
                          "lastName": "User",
                          "email": "updated@example.com"
                        }
                        """))
                .andRespond(withSuccess());
        server.expect(requestTo(adminBase + "/users/keycloak-user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id":"user-role-id","name":"USER"},
                          {"id":"offline-role-id","name":"offline_access"}
                        ]
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(adminBase + "/users/keycloak-user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().json("[{\"id\":\"user-role-id\",\"name\":\"USER\"}]"))
                .andRespond(withSuccess());
        server.expect(requestTo(adminBase + "/roles/STAFF"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"staff-role-id\",\"name\":\"STAFF\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(adminBase + "/users/keycloak-user-id/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[{\"id\":\"staff-role-id\",\"name\":\"STAFF\"}]"))
                .andRespond(withSuccess());

        UserCreDTO request = new UserCreDTO();
        request.setUsername("updated-user");
        request.setFirstname("Updated");
        request.setLastname("User");
        request.setEmail("updated@example.com");

        service.updateUser("keycloak-user-id", request, Role.STAFF);

        server.verify();
    }

    @Test
    void updateUserProfileDoesNotModifyRealmRoles() {
        String adminBase = "http://localhost:8180/admin/realms/my-workflow-dev";
        server.expect(requestTo("http://localhost:8180/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"admin-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(adminBase + "/users/keycloak-user-id"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {
                          "username": "current-user",
                          "firstName": "Updated",
                          "lastName": "Profile",
                          "email": "profile@example.com"
                        }
                        """))
                .andRespond(withSuccess());

        UserProfileUpdateDTO request = new UserProfileUpdateDTO();
        request.setFirstname("Updated");
        request.setLastname("Profile");
        request.setEmail("profile@example.com");

        service.updateUserProfile("keycloak-user-id", "current-user", request);

        server.verify();
    }
}
