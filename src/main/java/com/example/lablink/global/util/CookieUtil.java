package com.example.lablink.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "RefreshToken";

    @Value("${cookie.secure:false}")
    private boolean secure;

    @Value("${cookie.same-site:Lax}")
    private String sameSite;

    public void addRefreshTokenCookie(HttpServletResponse response, String token, long maxAgeSeconds) {
        String cookieValue = String.format(
            "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=%s%s",
            REFRESH_TOKEN_COOKIE_NAME, token, maxAgeSeconds, sameSite,
            secure ? "; Secure" : ""
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        String cookieValue = String.format(
            "%s=; Max-Age=0; Path=/; HttpOnly; SameSite=%s%s",
            REFRESH_TOKEN_COOKIE_NAME, sameSite,
            secure ? "; Secure" : ""
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
