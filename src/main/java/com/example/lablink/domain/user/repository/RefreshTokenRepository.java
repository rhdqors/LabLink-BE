package com.example.lablink.domain.user.repository;

import com.example.lablink.domain.user.entity.RefreshToken;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJtiAndRevokedAtIsNull(String jti);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revokedAt = :now WHERE r.subjectId = :subjectId AND r.subjectType = :subjectType AND r.revokedAt IS NULL")
    int revokeAllBySubject(@Param("subjectId") Long subjectId, @Param("subjectType") UserRoleEnum subjectType, @Param("now") LocalDateTime now);

    @Modifying
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
