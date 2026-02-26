package com.example.lablink.domain.auth.controller;

import com.example.lablink.domain.auth.service.AuthService;
import com.example.lablink.domain.company.security.CompanyDetailsImpl;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.security.UserDetailsImpl;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.message.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = authService.refreshToken(request, response);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, accessToken);
        return ResponseMessage.SuccessResponse("토큰 갱신 완료", null);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @AuthenticationPrincipal Object principal,
            HttpServletResponse response) {
        Long subjectId;
        UserRoleEnum subjectType;

        if (principal instanceof UserDetailsImpl) {
            subjectId = ((UserDetailsImpl) principal).getUser().getId();
            subjectType = UserRoleEnum.USER;
        } else if (principal instanceof CompanyDetailsImpl) {
            subjectId = ((CompanyDetailsImpl) principal).getCompany().getId();
            subjectType = UserRoleEnum.BUSINESS;
        } else {
            throw new GlobalException(GlobalErrorCode.INVALID_TOKEN);
        }

        authService.logout(subjectId, subjectType, response);
        response.setHeader(JwtUtil.AUTHORIZATION_HEADER, null);
        return ResponseMessage.SuccessResponse("로그아웃 완료", null);
    }
}
