package com.example.lablink.domain.user.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_jti", columnList = "jti"),
    @Index(name = "idx_subject", columnList = "subjectId, subjectType")
})
@Getter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRoleEnum subjectType;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime revokedAt;

    public RefreshToken(String jti, String tokenHash, Long subjectId, UserRoleEnum subjectType, LocalDateTime expiresAt) {
        this.jti = jti;
        this.tokenHash = tokenHash;
        this.subjectId = subjectId;
        this.subjectType = subjectType;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
}
