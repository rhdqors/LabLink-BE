package com.example.lablink.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CookieUtil Tests")
class CookieUtilTest {

    private final CookieUtil cookieUtil = new CookieUtil();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cookieUtil, "secure", false);
        ReflectionTestUtils.setField(cookieUtil, "sameSite", "Lax");
    }

    @Test
    @DisplayName("addRefreshTokenCookie는 HttpOnly 쿠키를 Set-Cookie 헤더에 추가한다")
    void addRefreshTokenCookie_setsHttpOnly() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(response, "test-token", 2592000);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("RefreshToken=test-token"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Max-Age=2592000"));
        assertTrue(setCookie.contains("Path=/"));
        assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("clearRefreshTokenCookie는 Max-Age=0으로 쿠키를 삭제한다")
    void clearRefreshTokenCookie_setsMaxAgeZero() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.clearRefreshTokenCookie(response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("RefreshToken="));
    }

    @Test
    @DisplayName("secure=true일 때 Secure 플래그가 포함된다")
    void addRefreshTokenCookie_secureFlag() {
        ReflectionTestUtils.setField(cookieUtil, "secure", true);
        ReflectionTestUtils.setField(cookieUtil, "sameSite", "None");
        MockHttpServletResponse response = new MockHttpServletResponse();

        cookieUtil.addRefreshTokenCookie(response, "test-token", 2592000);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=None"));
    }

    @Test
    @DisplayName("resolveRefreshToken은 쿠키에서 RT 값을 추출한다")
    void resolveRefreshToken_extractsFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", "my-rt-value"));

        String result = cookieUtil.resolveRefreshToken(request);
        assertEquals("my-rt-value", result);
    }

    @Test
    @DisplayName("resolveRefreshToken은 쿠키가 없으면 null을 반환한다")
    void resolveRefreshToken_returnsNullWhenNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        String result = cookieUtil.resolveRefreshToken(request);
        assertNull(result);
    }
}
