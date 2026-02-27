package com.example.lablink.domain.user.google.controller;

import com.example.lablink.domain.auth.service.AuthService;
import com.example.lablink.domain.oauth.enums.OAuthProvider;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.message.ResponseMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@Tag(name = "Google", description = "Google API")
@RestController
@Slf4j
@RequiredArgsConstructor
public class OauthController {

    private final AuthService authService;

    @GetMapping("/users/google/login")
    public ResponseEntity<?> googleLogin(
            @RequestParam String code,
            @RequestParam(required = false) String scope,
            HttpServletResponse response) {
        String accessToken = authService.processOAuthLogin(OAuthProvider.GOOGLE, code, null, response);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        return ResponseMessage.SuccessResponse("로그인 성공!", "");
    }
}
