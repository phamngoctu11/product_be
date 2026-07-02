package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.dto.UserResDTO;
import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import org.camunda.bpm.engine.RuntimeService;
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

import java.util.List;
import java.util.Map;
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
    private RuntimeService runtimeService;

    @Mock
    private KeycloakIdentityService keycloakIdentityService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrationAlwaysStartsProcessWithUserRole() {
        UserCreDTO dto = validUser();
        dto.setRole("ADMIN");
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("CreateUserProcess"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue().get("role")).isEqualTo("USER");
        assertThat(variablesCaptor.getValue().get("avatarUrl")).isEqualTo("https://example.com/avatar.png");
    }

    @Test
    void registrationAllowsAdminToAssignNonUserRole() {
        mockAuthenticatedRole("ADMIN");
        UserCreDTO dto = validUser();
        dto.setRole("staff");

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("CreateUserProcess"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue().get("role")).isEqualTo("STAFF");
    }

    @Test
    void registrationRejectsBlankPasswordBeforeStartingProcess() {
        UserCreDTO dto = validUser();
        dto.setPassword(" ");

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verifyNoInteractions(runtimeService);
    }

    @Test
    void registrationRejectsDuplicateUsernameBeforeStartingProcess() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByUsername("new-user")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasMessage("Tên đăng nhập đã tồn tại");

        verifyNoInteractions(runtimeService);
    }

    @Test
    void registrationRejectsDuplicateEmailBeforeStartingProcess() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);

        verifyNoInteractions(runtimeService);
    }

    @Test
    void registrationNormalizesBlankEmailToNull() {
        UserCreDTO dto = validUser();
        dto.setEmail("   ");

        userService.startUserRegistrationProcess(dto);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(runtimeService).startProcessInstanceByKey(eq("CreateUserProcess"), variablesCaptor.capture());
        assertThat(variablesCaptor.getValue().get("email")).isNull();
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void registrationRejectsDuplicatePhoneBeforeStartingProcess() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByPhone("0900000000")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasMessage("Số điện thoại đã tồn tại");

        verifyNoInteractions(runtimeService);
    }

    @Test
    void registrationRejectsInvalidRoleWhenCurrentUserCanAssignRole() {
        mockAuthenticatedRole("MANAGER");
        UserCreDTO dto = validUser();
        dto.setRole("OWNER");

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);

        verifyNoInteractions(runtimeService);
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
        verify(keycloakIdentityService).updateUser("customer", request);
        verify(userMapper).updateUser(user, request);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUserDisablesKeycloakUserAndSoftDeletesEntity() {
        User user = user("42", "customer");
        when(userRepository.findById("42")).thenReturn(Optional.of(user));

        userService.deleteUser("42");

        assertThat(user.isDelete()).isTrue();
        verify(keycloakIdentityService).disableUser("customer");
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
}
