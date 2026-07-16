package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserProfileUpdateDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private KeycloakIdentityService keycloakIdentityService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrationCreatesUserWithDefaultUserRole() {
        UserCreDTO dto = validUser();
        dto.setRole("ADMIN");
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(keycloakIdentityService.createUser(any(UserCreDTO.class), eq(Role.USER))).thenReturn("keycloak-id");

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo("keycloak-id");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(userCaptor.getValue().getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(userCaptor.getValue().getCart()).isNotNull();
        assertThat(userCaptor.getValue().getCart().getUser()).isSameAs(userCaptor.getValue());
    }

    @Test
    void registrationAllowsAdminToAssignNonUserRole() {
        mockAuthenticatedRole("ADMIN");
        UserCreDTO dto = validUser();
        dto.setRole("staff");
        when(keycloakIdentityService.createUser(any(UserCreDTO.class), eq(Role.STAFF))).thenReturn("staff-id");

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    void registrationRejectsBlankPasswordBeforeCreatingUser() {
        UserCreDTO dto = validUser();
        dto.setPassword(" ");

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void registrationRejectsDuplicateUsernameBeforeCreatingUser() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByUsername("new-user")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasMessage("Tên đăng nhập đã tồn tại");

        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void registrationRejectsDuplicateEmailBeforeCreatingUser() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void registrationNormalizesBlankEmailToNull() {
        UserCreDTO dto = validUser();
        dto.setEmail("   ");
        when(keycloakIdentityService.createUser(any(UserCreDTO.class), eq(Role.USER))).thenReturn("keycloak-id");

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isNull();
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void registrationRejectsDuplicatePhoneBeforeCreatingUser() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByPhone("0900000000")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasMessage("Số điện thoại đã tồn tại");

        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void registrationRejectsInvalidRoleWhenCurrentUserCanAssignRole() {
        mockAuthenticatedRole("MANAGER");
        UserCreDTO dto = validUser();
        dto.setRole("OWNER");

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    void getUserByIdReturnsMappedResponse() {
        User user = user("42", "customer");
        UserResDTO response = new UserResDTO();
        response.setId("42");
        response.setUsername("customer");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResDTO result = userService.getUserById("42");

        assertThat(result).isSameAs(response);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserSyncsKeycloakAndPersistsMappedEntity() {
        User user = user("42", "customer");
        UserCreDTO request = validUser();
        UserResDTO response = new UserResDTO();
        response.setId("42");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResDTO result = userService.updateUser("42", request);

        assertThat(result).isSameAs(response);
        verify(keycloakIdentityService).updateUser("42", request, Role.USER);
        verify(userMapper).updateUser(user, request);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserAllowsAdminToSynchronizeRoleWithKeycloak() {
        mockAuthenticatedRole("ADMIN");
        User user = user("42", "customer");
        UserCreDTO request = validUser();
        request.setRole("STAFF");
        UserResDTO response = new UserResDTO();
        when(userRepository.findById("42")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        userService.updateUser("42", request);

        assertThat(user.getRole()).isEqualTo(Role.STAFF);
        verify(keycloakIdentityService).updateUser("42", request, Role.STAFF);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRejectsRoleChangeFromUnprivilegedUser() {
        mockAuthenticatedRole("USER");
        User user = user("42", "customer");
        UserCreDTO request = validUser();
        request.setRole("STAFF");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.updateUser("42", request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);

        assertThat(user.getRole()).isEqualTo(Role.USER);
        verifyNoInteractions(keycloakIdentityService);
        verify(userMapper, never()).updateUser(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfileUsesJwtSubjectAndPreservesRole() {
        mockJwtUser("42", "customer");
        User user = user("42", "customer");
        user.setRole(Role.STAFF);
        UserProfileUpdateDTO request = new UserProfileUpdateDTO();
        request.setFirstname("Updated");
        request.setLastname("User");
        request.setGender("OTHER");
        request.setPhone("0911111111");
        request.setEmail("updated@example.com");
        UserResDTO response = new UserResDTO();
        when(userRepository.findById("42")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResDTO result = userService.updateMyProfile(request);

        assertThat(result).isSameAs(response);
        assertThat(user.getRole()).isEqualTo(Role.STAFF);
        verify(keycloakIdentityService).updateUserProfile("42", "customer", request);
        verify(userMapper).updateProfile(user, request);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserDisablesKeycloakUserAndSoftDeletesEntity() {
        User user = user("42", "customer");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));

        userService.deleteUser("42");

        assertThat(user.isDelete()).isTrue();
        verify(keycloakIdentityService).disableUser("42");
        verify(userRepository).save(user);
    }

    @Test
    void getUserByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById("42")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("42"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void appExceptionFormatsParameterizedMessage() {
        AppException exception = new AppException(
                HttpStatus.NOT_FOUND,
                ConstantErrorCode.USER_NOT_FOUND_WITH_ID,
                "42"
        );

        assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exception).hasMessage("Không tìm thấy người dùng có mã: 42");
    }

    private UserCreDTO validUser() {
        UserCreDTO dto = new UserCreDTO();
        dto.setUsername("new-user");
        dto.setPassword("secret123");
        dto.setFirstname("New");
        dto.setLastname("User");
        dto.setGender("OTHER");
        dto.setPhone("0900000000");
        dto.setEmail("user@example.com");
        dto.setAvatarUrl("https://example.com/avatar.png");
        return dto;
    }

    private User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(Role.USER);
        return user;
    }

    private void mockAuthenticatedRole(String authority) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        doReturn(List.of(new SimpleGrantedAuthority(authority))).when(authentication).getAuthorities();
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);
    }

    private void mockJwtUser(String subject, String username) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("USER")),
                username
        ));
        SecurityContextHolder.setContext(context);
    }
}
