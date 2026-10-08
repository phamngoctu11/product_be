package com.example.workflow.service;

import com.example.workflow.entity.User;
import com.example.workflow.exception.AppException;
import com.example.workflow.exception.ConstantErrorCode;
import com.example.workflow.nume.Role;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;

@Service("currentUserService")
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public Optional<String> findCurrentUserId() {
        return currentAuthentication().map(this::requireUserId);
    }

    public String requireCurrentUserId() {
        return requireUserId(requireAuthentication());
    }

    public Optional<User> findCurrentUser() {
        return currentAuthentication().map(this::requireUser);
    }

    public User requireCurrentUser() {
        return requireUser(requireAuthentication());
    }

    public User requireCurrentUser(Role role, ConstantErrorCode errorCode, Object... errorArgs) {
        return requireCurrentUser(Set.of(role), errorCode, errorArgs);
    }

    public User requireCurrentUser(Set<Role> roles, ConstantErrorCode errorCode, Object... errorArgs) {
        User user = requireCurrentUser();
        if (!roles.contains(user.getRole())) {
            throw new AppException(HttpStatus.FORBIDDEN, errorCode, errorArgs);
        }
        return user;
    }

    public String requireUserId(Authentication authentication) {
        Authentication authenticated = requireAuthenticated(authentication);
        if (authenticated.getPrincipal() instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) {
            return jwt.getSubject();
        }
        return findByUsername(authenticated).getId();
    }

    public boolean hasAuthority(String authority) {
        if (!StringUtils.hasText(authority)) {
            return false;
        }
        return currentAuthentication()
                .stream()
                .flatMap(authentication -> authentication.getAuthorities().stream())
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }

    public boolean isCurrentUserOwner(String ownerUserId) {
        return StringUtils.hasText(ownerUserId) && ownerUserId.equals(requireCurrentUserId());
    }

    private User requireUser(Authentication authentication) {
        Authentication authenticated = requireAuthenticated(authentication);
        if (authenticated.getPrincipal() instanceof Jwt jwt && StringUtils.hasText(jwt.getSubject())) {
            return userRepository.findById(jwt.getSubject())
                    .orElseGet(() -> findByUsername(authenticated));
        }
        return findByUsername(authenticated);
    }

    private User findByUsername(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, ConstantErrorCode.USER_NOT_FOUND));
    }

    private Authentication requireAuthentication() {
        return currentAuthentication()
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS));
    }

    private Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isAuthenticated(authentication) ? Optional.of(authentication) : Optional.empty();
    }

    private Authentication requireAuthenticated(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, ConstantErrorCode.INVALID_CREDENTIALS);
        }
        return authentication;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && StringUtils.hasText(authentication.getName())
                && !"anonymousUser".equals(authentication.getName());
    }
}
