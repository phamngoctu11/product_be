package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireCurrentUserIdUsesJwtSubjectWithoutQueryingDatabase() {
        CurrentUserService service = new CurrentUserService(userRepository);
        authenticate(jwtAuthentication("user-42", "customer", "USER"));

        assertThat(service.requireCurrentUserId()).isEqualTo("user-42");
        verifyNoInteractions(userRepository);
    }

    @Test
    void requireCurrentUserLoadsUserByJwtSubject() {
        CurrentUserService service = new CurrentUserService(userRepository);
        User user = user("user-42", "customer");
        authenticate(jwtAuthentication("user-42", "customer", "USER"));
        when(userRepository.findById("user-42")).thenReturn(Optional.of(user));

        assertThat(service.requireCurrentUser()).isSameAs(user);
        verify(userRepository).findById("user-42");
    }

    @Test
    void requireCurrentUserFallsBackToUsernameWhenJwtSubjectIsNotInLocalDatabase() {
        CurrentUserService service = new CurrentUserService(userRepository);
        User user = user("local-id", "customer");
        authenticate(jwtAuthentication("keycloak-id", "customer", "USER"));
        when(userRepository.findById("keycloak-id")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(user));

        assertThat(service.requireCurrentUser()).isSameAs(user);
    }

    @Test
    void nonJwtAuthenticationResolvesIdentityFromUsername() {
        CurrentUserService service = new CurrentUserService(userRepository);
        User user = user("user-7", "staff");
        authenticate(new UsernamePasswordAuthenticationToken(
                "staff",
                "ignored",
                List.of(new SimpleGrantedAuthority("STAFF"))
        ));
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(user));

        assertThat(service.requireCurrentUserId()).isEqualTo("user-7");
        assertThat(service.hasAuthority("STAFF")).isTrue();
    }

    @Test
    void anonymousAuthenticationIsNotTreatedAsCurrentUser() {
        CurrentUserService service = new CurrentUserService(userRepository);
        authenticate(new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        ));

        assertThat(service.findCurrentUserId()).isEmpty();
        assertThatThrownBy(service::requireCurrentUserId)
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(userRepository);
    }

    private void authenticate(org.springframework.security.core.Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private JwtAuthenticationToken jwtAuthentication(String subject, String username, String authority) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(authority)),
                username
        );
    }

    private User user(String id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
