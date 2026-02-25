package com.example.lablink.domain.user.kakao.controller;

import com.example.lablink.domain.user.kakao.service.KakaoService;
import com.example.lablink.global.message.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
public class KaKaoUserController {

    private final KakaoService kakaoService;

    @GetMapping("/kakao/login")
    public synchronized ResponseEntity<?> kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        kakaoService.kakaoLogin(code, response);
        return ResponseMessage.SuccessResponse("로그인 성공!", "");
    }
}
