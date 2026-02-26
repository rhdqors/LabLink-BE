package com.example.lablink.global.jwt;

import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Foundation Tests")
class SecurityFoundationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("JwtAuthenticationEntryPoint")
    class EntryPointTest {

        private JwtAuthenticationEntryPoint entryPoint;

        @BeforeEach
        void setUp() {
            entryPoint = new JwtAuthenticationEntryPoint();
        }

        @Test
        @DisplayName("인증 실패 시 401 상태코드와 'Token Error' 메시지를 JSON으로 반환한다")
        void commence_Returns401WithTokenErrorMessage() throws IOException, ServletException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            entryPoint.commence(request, response,
                    new org.springframework.security.authentication.BadCredentialsException("test"));

            // then
            assertEquals(401, response.getStatus());
            assertEquals("application/json;charset=UTF-8", response.getContentType());

            String json = response.getContentAsString();
            var jsonNode = objectMapper.readTree(json);
            assertEquals(401, jsonNode.get("statusCode").asInt());
            assertEquals("Token Error", jsonNode.get("message").asText());
        }
    }

    @Nested
    @DisplayName("JwtAccessDeniedHandler")
    class AccessDeniedHandlerTest {

        private JwtAccessDeniedHandler handler;

        @BeforeEach
        void setUp() {
            handler = new JwtAccessDeniedHandler();
        }

        @Test
        @DisplayName("접근 거부 시 403 상태코드와 접근 권한 없음 메시지를 JSON으로 반환한다")
        void handle_Returns403WithAccessDeniedMessage() throws IOException, ServletException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            handler.handle(request, response,
                    new org.springframework.security.access.AccessDeniedException("test"));

            // then
            assertEquals(403, response.getStatus());
            assertEquals("application/json;charset=UTF-8", response.getContentType());

            String json = response.getContentAsString();
            var jsonNode = objectMapper.readTree(json);
            assertEquals(403, jsonNode.get("statusCode").asInt());
            assertEquals("접근 권한이 없습니다", jsonNode.get("message").asText());
        }
    }

    @Nested
    @DisplayName("JwtAuthFilter")
    class JwtAuthFilterTest {

        @Mock
        private JwtUtil jwtUtil;

        @Mock
        private FilterChain filterChain;

        private JwtAuthFilter jwtAuthFilter;

        @BeforeEach
        void setUp() {
            jwtAuthFilter = new JwtAuthFilter(jwtUtil);
            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("유효하지 않은 토큰이 있어도 filterChain.doFilter가 호출된다 (pass-through)")
        void invalidToken_ShouldPassThrough() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer invalid-token");
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(jwtUtil.resolveToken(any())).thenReturn("invalid-token");
            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보가 설정된다")
        void validToken_ShouldSetAuthentication() throws ServletException, IOException {
            // given
            Authentication mockAuth = mock(Authentication.class);
            when(jwtUtil.createAuthentication("1", "USER")).thenReturn(mockAuth);

            // when - setAuthentication is the core logic called from doFilterInternal
            // after token validation and claims parsing succeed
            jwtAuthFilter.setAuthentication("1", "USER");

            // then
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(mockAuth, SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("토큰이 없으면 인증 없이 통과한다")
        void noToken_ShouldPassThroughWithoutError() throws ServletException, IOException {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            when(jwtUtil.resolveToken(any())).thenReturn(null);

            // when
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("UserDetailsImpl")
    class UserDetailsImplTest {

        @Test
        @DisplayName("isEnabled()가 true를 반환한다")
        void isEnabled_ReturnsTrue() {
            // given
            User user = new User();
            user.setRole(UserRoleEnum.USER);
            UserDetailsImpl userDetails = new UserDetailsImpl(user, "1");

            // when & then
            assertTrue(userDetails.isEnabled());
        }

        @Test
        @DisplayName("isAccountNonExpired()가 true를 반환한다")
        void isAccountNonExpired_ReturnsTrue() {
            // given
            User user = new User();
            user.setRole(UserRoleEnum.USER);
            UserDetailsImpl userDetails = new UserDetailsImpl(user, "1");

            // when & then
            assertTrue(userDetails.isAccountNonExpired());
        }

        @Test
        @DisplayName("isAccountNonLocked()가 true를 반환한다")
        void isAccountNonLocked_ReturnsTrue() {
            // given
            User user = new User();
            user.setRole(UserRoleEnum.USER);
            UserDetailsImpl userDetails = new UserDetailsImpl(user, "1");

            // when & then
            assertTrue(userDetails.isAccountNonLocked());
        }

        @Test
        @DisplayName("isCredentialsNonExpired()가 true를 반환한다")
        void isCredentialsNonExpired_ReturnsTrue() {
            // given
            User user = new User();
            user.setRole(UserRoleEnum.USER);
            UserDetailsImpl userDetails = new UserDetailsImpl(user, "1");

            // when & then
            assertTrue(userDetails.isCredentialsNonExpired());
        }
    }
}
