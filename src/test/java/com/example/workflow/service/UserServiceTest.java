package com.example.workflow.service;

import com.example.workflow.dto.UserCreDTO;
import com.example.workflow.exception.AppException;
import com.example.workflow.mapper.UserMapper;
import com.example.workflow.repository.UserRepository;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
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

    @InjectMocks
    private UserService userService;

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
    void registrationRejectsDuplicateUsernameBeforeStartingProcess() {
        UserCreDTO dto = validUser();
        when(userRepository.existsByUsername("new-user")).thenReturn(true);

        assertThatThrownBy(() -> userService.startUserRegistrationProcess(dto))
                .isInstanceOf(AppException.class)
                .hasMessage("Username already exists");

        verifyNoInteractions(runtimeService);
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
}
