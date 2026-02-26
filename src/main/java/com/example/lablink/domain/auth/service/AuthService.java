package com.example.lablink.domain.auth.service;

import com.example.lablink.domain.company.entity.Company;
import com.example.lablink.domain.company.repository.CompanyRepository;
import com.example.lablink.domain.user.entity.RefreshToken;
import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.repository.RefreshTokenRepository;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.util.CookieUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @Transactional
    public String generateAndStoreRefreshToken(Long subjectId, UserRoleEnum subjectType) {
        String jti = UUID.randomUUID().toString();
        String rawToken = jwtUtil.createRefreshToken(jti);
        String tokenHash = sha256(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plus(Duration.ofMillis(JwtUtil.RF_TOKEN_TIME));
        RefreshToken refreshToken = new RefreshToken(jti, tokenHash, subjectId, subjectType, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawToken = cookieUtil.resolveRefreshToken(request);
        if (rawToken == null) {
            throw new GlobalException(GlobalErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        String jti = jwtUtil.extractJti(rawToken);
        if (jti == null) {
            throw new GlobalException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByJtiAndRevokedAtIsNull(jti)
            .orElseThrow(() -> new GlobalException(GlobalErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.isExpired()) {
            throw new GlobalException(GlobalErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        String tokenHash = sha256(rawToken);
        if (!tokenHash.equals(storedToken.getTokenHash())) {
            throw new GlobalException(GlobalErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Rotation: revoke old, issue new
        storedToken.revoke();

        Long subjectId = storedToken.getSubjectId();
        UserRoleEnum subjectType = storedToken.getSubjectType();

        String newRawToken = generateAndStoreRefreshToken(subjectId, subjectType);
        cookieUtil.addRefreshTokenCookie(response, newRawToken, JwtUtil.RF_TOKEN_TIME / 1000);

        // Issue new Access Token (nickname/companyName claim 포함)
        String accessToken = createAccessToken(subjectId, subjectType);
        return accessToken;
    }

    @Transactional
    public void logout(Long subjectId, UserRoleEnum subjectType, HttpServletResponse response) {
        refreshTokenRepository.revokeAllBySubject(subjectId, subjectType, LocalDateTime.now());
        cookieUtil.clearRefreshTokenCookie(response);
    }

    private String createAccessToken(Long subjectId, UserRoleEnum subjectType) {
        if (subjectType == UserRoleEnum.USER) {
            User user = userRepository.findById(subjectId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
            return jwtUtil.createUserToken(user);
        } else {
            Company company = companyRepository.findById(subjectId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.COMPANY_NOT_FOUND));
            return jwtUtil.createCompanyToken(company);
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다", e);
        }
    }
}
