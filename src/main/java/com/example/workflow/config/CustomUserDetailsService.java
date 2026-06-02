package com.example.workflow.config;
import com.example.workflow.entity.User;
import com.example.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Tìm user trong database
        User user = userRepository.findByUsernameAndIsDeleteFalse(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // 2. Chuyển đổi một Role duy nhất thành danh sách Authority của Spring Security
        // Vì user.getRole() bây giờ trả về 1 đối tượng đơn lẻ, không phải Collection, nên không dùng .stream() được
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(toAuthority(user.getRole().name()))
        );

        // 3. Trả về đối tượng UserDetails cho Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    private String toAuthority(String role) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase();
        return normalizedRole.startsWith("ROLE_") ? normalizedRole.substring("ROLE_".length()) : normalizedRole;
    }
}
