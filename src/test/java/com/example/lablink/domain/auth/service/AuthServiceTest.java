package com.example.lablink.domain.auth.service;

import com.example.lablink.domain.company.entity.Company;
import com.example.lablink.domain.company.repository.CompanyRepository;
import com.example.lablink.domain.user.entity.RefreshToken;
import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.repository.RefreshTokenRepository;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.util.CookieUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CookieUtil cookieUtil;

    @Nested
    @DisplayName("RefreshToken Entity")
    class RefreshTokenEntityTest {

        @Test
        @DisplayName("revoke() 호출 시 revokedAt이 설정된다")
        void revoke_setsRevokedAt() {
            RefreshToken token = new RefreshToken("jti-1", "hash", 1L, UserRoleEnum.USER,
                    LocalDateTime.now().plusDays(30));

            assertFalse(token.isRevoked());
            token.revoke();
            assertTrue(token.isRevoked());
            assertNotNull(token.getRevokedAt());
        }

        @Test
        @DisplayName("만료된 토큰은 isExpired()가 true를 반환한다")
        void isExpired_returnsTrueWhenPast() {
            RefreshToken token = new RefreshToken("jti-2", "hash", 1L, UserRoleEnum.USER,
                    LocalDateTime.now().minusMinutes(1));

            assertTrue(token.isExpired());
        }

        @Test
        @DisplayName("유효한 토큰은 isExpired()가 false를 반환한다")
        void isExpired_returnsFalseWhenFuture() {
            RefreshToken token = new RefreshToken("jti-3", "hash", 1L, UserRoleEnum.USER,
                    LocalDateTime.now().plusDays(30));

            assertFalse(token.isExpired());
        }
    }

    @Nested
    @DisplayName("generateAndStoreRefreshToken")
    class GenerateRefreshTokenTest {

        @Test
        @DisplayName("RT 생성 시 DB에 저장하고 원본 JWT를 반환한다")
        void generatesAndStoresToken() {
            given(jwtUtil.createRefreshToken(anyString())).willReturn("raw-jwt-token");
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(i -> i.getArgument(0));

            String result = authService.generateAndStoreRefreshToken(1L, UserRoleEnum.USER);

            assertEquals("raw-jwt-token", result);
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertNotNull(saved.getJti());
            assertNotNull(saved.getTokenHash());
            assertEquals(1L, saved.getSubjectId());
            assertEquals(UserRoleEnum.USER, saved.getSubjectType());
        }
    }

    @Nested
    @DisplayName("refreshToken (Rotation)")
    class RefreshTokenRotationTest {

        @Test
        @DisplayName("유효한 RT로 갱신 시 기존 토큰이 revoke되고 새 토큰이 발급된다")
        void validToken_rotates() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            String rawToken = "raw-token";
            String tokenHash = sha256(rawToken);
            RefreshToken storedToken = new RefreshToken("jti-old", tokenHash, 1L, UserRoleEnum.USER,
                    LocalDateTime.now().plusDays(30));
            User user = new User();

            given(cookieUtil.resolveRefreshToken(request)).willReturn(rawToken);
            given(jwtUtil.extractJti(rawToken)).willReturn("jti-old");
            given(refreshTokenRepository.findByJtiAndRevokedAtIsNull("jti-old")).willReturn(Optional.of(storedToken));
            given(jwtUtil.createRefreshToken(anyString())).willReturn("new-raw-token");
            given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(i -> i.getArgument(0));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(jwtUtil.createUserToken(user)).willReturn("Bearer new-access-token");

            String result = authService.refreshToken(request, response);

            assertEquals("Bearer new-access-token", result);
            assertTrue(storedToken.isRevoked());
            verify(cookieUtil).addRefreshTokenCookie(eq(response), eq("new-raw-token"), anyLong());
            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("revoke된 RT로 갱신 시 예외가 발생한다")
        void revokedToken_throws() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(cookieUtil.resolveRefreshToken(request)).willReturn("raw-token");
            given(jwtUtil.extractJti("raw-token")).willReturn("jti-revoked");
            given(refreshTokenRepository.findByJtiAndRevokedAtIsNull("jti-revoked")).willReturn(Optional.empty());

            assertThrows(GlobalException.class, () ->
                    authService.refreshToken(request, response));
        }

        @Test
        @DisplayName("쿠키에 RT가 없으면 예외가 발생한다")
        void noToken_throws() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(cookieUtil.resolveRefreshToken(request)).willReturn(null);

            assertThrows(GlobalException.class, () ->
                    authService.refreshToken(request, response));
        }

        @Test
        @DisplayName("만료된 RT로 갱신 시 예외가 발생한다")
        void expiredToken_throws() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            String rawToken = "expired-raw-token";
            String tokenHash = sha256(rawToken);
            RefreshToken expiredToken = new RefreshToken("jti-expired", tokenHash, 1L, UserRoleEnum.USER,
                    LocalDateTime.now().minusMinutes(1));

            given(cookieUtil.resolveRefreshToken(request)).willReturn(rawToken);
            given(jwtUtil.extractJti(rawToken)).willReturn("jti-expired");
            given(refreshTokenRepository.findByJtiAndRevokedAtIsNull("jti-expired")).willReturn(Optional.of(expiredToken));

            assertThrows(GlobalException.class, () ->
                    authService.refreshToken(request, response));
        }

        @Test
        @DisplayName("해시 불일치 RT로 갱신 시 예외가 발생한다")
        void hashMismatch_throws() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            RefreshToken storedToken = new RefreshToken("jti-hash", "wrong-hash-value", 1L, UserRoleEnum.USER,
                    LocalDateTime.now().plusDays(30));

            given(cookieUtil.resolveRefreshToken(request)).willReturn("raw-token");
            given(jwtUtil.extractJti("raw-token")).willReturn("jti-hash");
            given(refreshTokenRepository.findByJtiAndRevokedAtIsNull("jti-hash")).willReturn(Optional.of(storedToken));

            assertThrows(GlobalException.class, () ->
                    authService.refreshToken(request, response));
        }
    }

    private static String sha256(String input) {
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
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 시 해당 subject의 모든 RT가 revoke되고 쿠키가 클리어된다")
        void revokesAllAndClearsCookie() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            given(refreshTokenRepository.revokeAllBySubject(eq(1L), eq(UserRoleEnum.USER), any(LocalDateTime.class)))
                    .willReturn(2);

            authService.logout(1L, UserRoleEnum.USER, response);

            verify(refreshTokenRepository).revokeAllBySubject(eq(1L), eq(UserRoleEnum.USER), any(LocalDateTime.class));
            verify(cookieUtil).clearRefreshTokenCookie(response);
        }
    }
}
