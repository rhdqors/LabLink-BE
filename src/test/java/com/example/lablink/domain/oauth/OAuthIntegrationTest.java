package com.example.lablink.domain.oauth;

import com.example.lablink.domain.auth.service.AuthService;
import com.example.lablink.domain.company.repository.CompanyRepository;
import com.example.lablink.domain.oauth.client.OAuthClient;
import com.example.lablink.domain.oauth.dto.OAuthUserInfo;
import com.example.lablink.domain.oauth.enums.OAuthProvider;
import com.example.lablink.domain.oauth.service.OAuthStateStore;
import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserInfo;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.repository.RefreshTokenRepository;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.domain.user.service.UserInfoService;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth Integration Tests")
class OAuthIntegrationTest {

    // ──────────────────────────────────────────────
    // OAuthStateStore Tests
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("OAuthStateStore")
    class OAuthStateStoreTest {

        @Mock
        private RedisTemplate<String, Object> redisTemplate;

        @Mock
        private ValueOperations<String, Object> valueOperations;

        private OAuthStateStore stateStore;

        @BeforeEach
        void setUp() {
            stateStore = new OAuthStateStore(redisTemplate);
        }

        @Test
        @DisplayName("state 생성 시 UUID를 Redis에 저장하고 반환한다")
        void generateState_storesInRedisAndReturns() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            String state = stateStore.generateState(OAuthProvider.KAKAO);

            assertNotNull(state);
            assertFalse(state.isEmpty());
            verify(valueOperations).set(
                    eq("oauth_state:" + state),
                    eq("KAKAO"),
                    eq(5L),
                    eq(TimeUnit.MINUTES)
            );
        }

        @Test
        @DisplayName("유효한 state 검증 시 true를 반환하고 Redis에서 원자적으로 삭제한다")
        void validateAndConsume_validState_returnsTrue() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("oauth_state:test-state")).willReturn("KAKAO");

            boolean result = stateStore.validateAndConsume("test-state", OAuthProvider.KAKAO);

            assertTrue(result);
            verify(valueOperations).getAndDelete("oauth_state:test-state");
        }

        @Test
        @DisplayName("다른 provider로 검증 시 false를 반환한다")
        void validateAndConsume_wrongProvider_returnsFalse() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("oauth_state:test-state")).willReturn("KAKAO");

            boolean result = stateStore.validateAndConsume("test-state", OAuthProvider.GOOGLE);

            assertFalse(result);
        }

        @Test
        @DisplayName("존재하지 않는 state 검증 시 false를 반환한다")
        void validateAndConsume_unknownState_returnsFalse() {
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.getAndDelete("oauth_state:unknown")).willReturn(null);

            boolean result = stateStore.validateAndConsume("unknown", OAuthProvider.KAKAO);

            assertFalse(result);
        }
    }

    // ──────────────────────────────────────────────
    // OAuthClient Strategy Resolution Tests
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("OAuthClient Strategy Resolution")
    class OAuthClientStrategyTest {

        @Mock private RefreshTokenRepository refreshTokenRepository;
        @Mock private UserRepository userRepository;
        @Mock private CompanyRepository companyRepository;
        @Mock private JwtUtil jwtUtil;
        @Mock private CookieUtil cookieUtil;
        @Mock private OAuthClient kakaoClient;
        @Mock private OAuthClient googleClient;
        @Mock private OAuthStateStore stateStore;
        @Mock private UserInfoService userInfoService;
        @Mock private PasswordEncoder passwordEncoder;

        private AuthService authService;

        @BeforeEach
        void setUp() {
            lenient().when(kakaoClient.getProvider()).thenReturn(OAuthProvider.KAKAO);
            lenient().when(googleClient.getProvider()).thenReturn(OAuthProvider.GOOGLE);
            authService = new AuthService(
                    refreshTokenRepository, userRepository, companyRepository,
                    jwtUtil, cookieUtil,
                    List.of(kakaoClient, googleClient),
                    stateStore, userInfoService, passwordEncoder
            );
        }

        @Test
        @DisplayName("KAKAO provider로 processOAuthLogin 호출 시 KakaoClient가 사용된다")
        void resolves_kakao_client() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User kakaoUser = new User();
            kakaoUser.setId(1L);
            kakaoUser.setNickName("kakaoNick");
            kakaoUser.setRole(UserRoleEnum.USER);

            given(kakaoClient.getAccessToken("kakao-code")).willReturn("kakao-at");
            given(kakaoClient.getUserInfo("kakao-at")).willReturn(
                    new OAuthUserInfo("12345", "kakao@test.com", "kakaoNick", OAuthProvider.KAKAO));
            given(userRepository.findByKakaoId(12345L)).willReturn(Optional.of(kakaoUser));
            given(jwtUtil.createUserToken(kakaoUser)).willReturn("Bearer jwt-token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt-token");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.KAKAO, "kakao-code", null, response);

            assertEquals("Bearer jwt-token", result);
            verify(kakaoClient).getAccessToken("kakao-code");
            verify(googleClient, never()).getAccessToken(any());
        }

        @Test
        @DisplayName("GOOGLE provider로 processOAuthLogin 호출 시 GoogleClient가 사용된다")
        void resolves_google_client() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User googleUser = new User();
            googleUser.setId(2L);
            googleUser.setNickName("googleNick");
            googleUser.setRole(UserRoleEnum.USER);

            given(googleClient.getAccessToken("google-code")).willReturn("google-at");
            given(googleClient.getUserInfo("google-at")).willReturn(
                    new OAuthUserInfo("sub-123", "google@test.com", "googleNick", OAuthProvider.GOOGLE));
            given(userRepository.findByGoogleEmail("google@test.com")).willReturn(Optional.of(googleUser));
            given(jwtUtil.createUserToken(googleUser)).willReturn("Bearer jwt-token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt-token");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.GOOGLE, "google-code", null, response);

            assertEquals("Bearer jwt-token", result);
            verify(googleClient).getAccessToken("google-code");
            verify(kakaoClient, never()).getAccessToken(any());
        }
    }

    // ──────────────────────────────────────────────
    // processOAuthLogin Flow Tests
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("processOAuthLogin")
    class ProcessOAuthLoginTest {

        @Mock private RefreshTokenRepository refreshTokenRepository;
        @Mock private UserRepository userRepository;
        @Mock private CompanyRepository companyRepository;
        @Mock private JwtUtil jwtUtil;
        @Mock private CookieUtil cookieUtil;
        @Mock private OAuthClient kakaoClient;
        @Mock private OAuthClient googleClient;
        @Mock private OAuthStateStore stateStore;
        @Mock private UserInfoService userInfoService;
        @Mock private PasswordEncoder passwordEncoder;

        private AuthService authService;

        @BeforeEach
        void setUp() {
            lenient().when(kakaoClient.getProvider()).thenReturn(OAuthProvider.KAKAO);
            lenient().when(googleClient.getProvider()).thenReturn(OAuthProvider.GOOGLE);
            authService = new AuthService(
                    refreshTokenRepository, userRepository, companyRepository,
                    jwtUtil, cookieUtil,
                    List.of(kakaoClient, googleClient),
                    stateStore, userInfoService, passwordEncoder
            );
        }

        @Test
        @DisplayName("state가 유효하면 OAuth 로그인이 성공한다")
        void validState_loginSucceeds() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User user = new User();
            user.setId(1L);
            user.setNickName("nick");
            user.setRole(UserRoleEnum.USER);

            given(stateStore.validateAndConsume("valid-state", OAuthProvider.KAKAO)).willReturn(true);
            given(kakaoClient.getAccessToken("code")).willReturn("oauth-at");
            given(kakaoClient.getUserInfo("oauth-at")).willReturn(
                    new OAuthUserInfo("12345", "test@kakao.com", "nick", OAuthProvider.KAKAO));
            given(userRepository.findByKakaoId(12345L)).willReturn(Optional.of(user));
            given(jwtUtil.createUserToken(user)).willReturn("Bearer access-token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt-value");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.KAKAO, "code", "valid-state", response);

            assertEquals("Bearer access-token", result);
            verify(stateStore).validateAndConsume("valid-state", OAuthProvider.KAKAO);
            verify(cookieUtil).addRefreshTokenCookie(eq(response), eq("rt-value"), anyLong());
        }

        @Test
        @DisplayName("state가 유효하지 않으면 예외가 발생한다")
        void invalidState_throws() {
            MockHttpServletResponse response = new MockHttpServletResponse();
            given(stateStore.validateAndConsume("bad-state", OAuthProvider.KAKAO)).willReturn(false);

            assertThrows(GlobalException.class, () ->
                    authService.processOAuthLogin(OAuthProvider.KAKAO, "code", "bad-state", response));
        }

        @Test
        @DisplayName("state가 null이면 검증을 건너뛴다 (레거시 모드)")
        void nullState_skipsValidation() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User user = new User();
            user.setId(1L);
            user.setNickName("nick");
            user.setRole(UserRoleEnum.USER);

            given(kakaoClient.getAccessToken("code")).willReturn("oauth-at");
            given(kakaoClient.getUserInfo("oauth-at")).willReturn(
                    new OAuthUserInfo("12345", null, "nick", OAuthProvider.KAKAO));
            given(userRepository.findByKakaoId(12345L)).willReturn(Optional.of(user));
            given(jwtUtil.createUserToken(user)).willReturn("Bearer token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.KAKAO, "code", null, response);

            assertNotNull(result);
            verify(stateStore, never()).validateAndConsume(any(), any());
        }

        @Test
        @DisplayName("신규 카카오 사용자는 자동 가입 후 토큰이 발급된다")
        void newKakaoUser_autoSignupAndTokenIssued() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User newUser = new User();
            newUser.setId(10L);
            newUser.setNickName("kakaoNick");
            newUser.setRole(UserRoleEnum.USER);

            given(kakaoClient.getAccessToken("code")).willReturn("oauth-at");
            given(kakaoClient.getUserInfo("oauth-at")).willReturn(
                    new OAuthUserInfo("99999", "new@kakao.com", "kakaoNick", OAuthProvider.KAKAO));
            given(userRepository.findByKakaoId(99999L)).willReturn(Optional.empty());
            given(userInfoService.saveKakaoUserInfo()).willReturn(new UserInfo());
            given(userRepository.save(any(User.class))).willReturn(newUser);
            given(jwtUtil.createUserToken(newUser)).willReturn("Bearer new-token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.KAKAO, "code", null, response);

            assertEquals("Bearer new-token", result);
            verify(userInfoService).saveKakaoUserInfo();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("신규 구글 사용자는 자동 가입 후 토큰이 발급된다")
        void newGoogleUser_autoSignupAndTokenIssued() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User newUser = new User();
            newUser.setId(20L);
            newUser.setNickName("googleNick");
            newUser.setRole(UserRoleEnum.USER);

            given(googleClient.getAccessToken("code")).willReturn("oauth-at");
            given(googleClient.getUserInfo("oauth-at")).willReturn(
                    new OAuthUserInfo("sub-new", "new@google.com", "googleNick", OAuthProvider.GOOGLE));
            given(userRepository.findByGoogleEmail("new@google.com")).willReturn(Optional.empty());
            given(userRepository.findByEmail("new@google.com")).willReturn(Optional.empty());
            given(userInfoService.saveKakaoUserInfo()).willReturn(new UserInfo());
            given(passwordEncoder.encode(anyString())).willReturn("encoded-pw");
            given(userRepository.save(any(User.class))).willReturn(newUser);
            given(jwtUtil.createUserToken(newUser)).willReturn("Bearer google-token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            String result = authService.processOAuthLogin(OAuthProvider.GOOGLE, "code", null, response);

            assertEquals("Bearer google-token", result);
            verify(userInfoService).saveKakaoUserInfo();
            verify(passwordEncoder).encode(anyString());
        }
    }

    // ──────────────────────────────────────────────
    // Google findById Bug Fix Test
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("Google findById 버그 수정")
    class GoogleBugFixTest {

        @Mock private RefreshTokenRepository refreshTokenRepository;
        @Mock private UserRepository userRepository;
        @Mock private CompanyRepository companyRepository;
        @Mock private JwtUtil jwtUtil;
        @Mock private CookieUtil cookieUtil;
        @Mock private OAuthClient googleClient;
        @Mock private OAuthStateStore stateStore;
        @Mock private UserInfoService userInfoService;
        @Mock private PasswordEncoder passwordEncoder;

        private AuthService authService;

        @BeforeEach
        void setUp() {
            lenient().when(googleClient.getProvider()).thenReturn(OAuthProvider.GOOGLE);
            authService = new AuthService(
                    refreshTokenRepository, userRepository, companyRepository,
                    jwtUtil, cookieUtil,
                    List.of(googleClient),
                    stateStore, userInfoService, passwordEncoder
            );
        }

        @Test
        @DisplayName("구글 로그인 시 findByGoogleEmail로 사용자를 조회한다 (findById 아님)")
        void findsUserByGoogleEmail_notById() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User existingUser = new User();
            existingUser.setId(5L);
            existingUser.setNickName("existing");
            existingUser.setRole(UserRoleEnum.USER);

            given(googleClient.getAccessToken("code")).willReturn("at");
            given(googleClient.getUserInfo("at")).willReturn(
                    new OAuthUserInfo("109877234234234", "user@gmail.com", "existing", OAuthProvider.GOOGLE));
            given(userRepository.findByGoogleEmail("user@gmail.com")).willReturn(Optional.of(existingUser));
            given(jwtUtil.createUserToken(existingUser)).willReturn("Bearer token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            authService.processOAuthLogin(OAuthProvider.GOOGLE, "code", null, response);

            // 핵심 검증: findByGoogleEmail 호출, findById 호출 안 함
            verify(userRepository).findByGoogleEmail("user@gmail.com");
            verify(userRepository, never()).findById(109877234234234L);
        }

        @Test
        @DisplayName("기존 이메일 사용자가 구글 로그인 시 계정이 연동된다")
        void existingEmailUser_linksGoogleAccount() {
            MockHttpServletResponse response = new MockHttpServletResponse();

            User existingUser = new User();
            existingUser.setId(7L);
            existingUser.setEmail("shared@gmail.com");
            existingUser.setNickName("normalUser");
            existingUser.setRole(UserRoleEnum.USER);

            given(googleClient.getAccessToken("code")).willReturn("at");
            given(googleClient.getUserInfo("at")).willReturn(
                    new OAuthUserInfo("sub-111", "shared@gmail.com", "GoogleName", OAuthProvider.GOOGLE));
            given(userRepository.findByGoogleEmail("shared@gmail.com")).willReturn(Optional.empty());
            given(userRepository.findByEmail("shared@gmail.com")).willReturn(Optional.of(existingUser));
            given(userRepository.save(existingUser)).willReturn(existingUser);
            given(jwtUtil.createUserToken(existingUser)).willReturn("Bearer token");
            given(jwtUtil.createRefreshToken(anyString())).willReturn("rt");
            given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

            authService.processOAuthLogin(OAuthProvider.GOOGLE, "code", null, response);

            // 계정 연동 검증: googleEmail이 설정됨
            assertEquals("shared@gmail.com", existingUser.getGoogleEmail());
            verify(userRepository).save(existingUser);
        }
    }

    // ──────────────────────────────────────────────
    // OAuthProvider enum Tests
    // ──────────────────────────────────────────────
    @Nested
    @DisplayName("OAuthProvider")
    class OAuthProviderTest {

        @Test
        @DisplayName("유효한 provider 문자열로 enum을 생성한다")
        void from_validProvider() {
            assertEquals(OAuthProvider.KAKAO, OAuthProvider.from("kakao"));
            assertEquals(OAuthProvider.GOOGLE, OAuthProvider.from("google"));
            assertEquals(OAuthProvider.KAKAO, OAuthProvider.from("KAKAO"));
        }

        @Test
        @DisplayName("지원하지 않는 provider 문자열은 예외를 발생시킨다")
        void from_invalidProvider_throws() {
            assertThrows(GlobalException.class, () -> OAuthProvider.from("naver"));
            assertThrows(GlobalException.class, () -> OAuthProvider.from(""));
        }
    }
}
