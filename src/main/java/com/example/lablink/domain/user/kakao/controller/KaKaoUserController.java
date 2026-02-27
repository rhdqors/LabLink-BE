package com.example.lablink.domain.user.kakao.controller;

import com.example.lablink.domain.auth.service.AuthService;
import com.example.lablink.domain.oauth.enums.OAuthProvider;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.message.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class KaKaoUserController {

    private final AuthService authService;

    @GetMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestParam String code, HttpServletResponse response) {
        String accessToken = authService.processOAuthLogin(OAuthProvider.KAKAO, code, null, response);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        return ResponseMessage.SuccessResponse("로그인 성공!", "");
    }
}
