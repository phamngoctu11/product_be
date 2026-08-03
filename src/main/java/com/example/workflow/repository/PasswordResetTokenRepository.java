package com.example.workflow.repository;

import com.example.workflow.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    List<PasswordResetToken> findByUser_IdAndUsedAtIsNull(String userId);
}
