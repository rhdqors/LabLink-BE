package com.example.lablink.domain.user.google.service;

import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.domain.user.google.dto.GoogleUserInfoDto;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
public class OauthService {
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void googleLogin(String code, String scope, HttpServletResponse response) throws JsonProcessingException {
        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(code, scope);

        // 2. 토큰으로 Google API 호출 : "액세스 토큰"으로 "Google 사용자 정보" 가져오기
        GoogleUserInfoDto googleUserInfo = getGoogleUserInfo(accessToken);

        // 3. 필요시에 회원가입
        registerGoogleUserIfNeeded(googleUserInfo);

        // 4. JWT 토큰 생성 및 응답 헤더에 추가
        User googleUser = userRepository.findByGoogleEmail(googleUserInfo.getEmail())
            .orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createUserToken(googleUser));
    }


    // 1. "인가 코드"로 "액세스 토큰" 요청
    private String getToken(String code, String scope) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("scope", scope);
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> googleTokenRequest =
            new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
            "https://oauth2.googleapis.com/token",
//            "https://www.googleapis.com/oauth2/v4/token",
//            "https://accounts.google.com/o/oauth2/token",
            HttpMethod.POST,
            googleTokenRequest,
            String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        log.debug("Google OAuth response body: {}", responseBody);
        log.debug("Google OAuth json node: {}", jsonNode);
        return jsonNode.get("access_token").asText();
    }

    // 2. 토큰으로 Google API 호출 : "액세스 토큰"으로 "Google 사용자 정보" 가져오기
    private GoogleUserInfoDto getGoogleUserInfo(String accessToken) throws JsonProcessingException {
        // Create HTTP Header
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken); // Add space after "Bearer"
        headers.add("Content-type", "application/x-www-form-urlencoded; charset=utf-8");

        // send HTTP request
        HttpEntity<MultiValueMap<String, String>> googleUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        String userInfoEndpoint = "https://www.googleapis.com/oauth2/v3/userinfo"; // Set the correct user info endpoint
        ResponseEntity<String> response = rt.exchange(
            userInfoEndpoint,
            HttpMethod.POST,
            googleUserInfoRequest,
            String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        Long id = jsonNode.get("sub").asLong();
        String nickname = jsonNode.get("name").asText();
        String email = jsonNode.get("email").asText();

        log.info("Google user information: " + id + ", " + nickname + ", " + email);
        return new GoogleUserInfoDto(id, nickname, email);
    }


    // 3. 필요시에 회원가입
    private void registerGoogleUserIfNeeded(GoogleUserInfoDto googleUserInfo) {
        // DB 에 중복된 구글 Id 가 있는지 확인
        Long googleId = googleUserInfo.getId();
        User googleUser = userRepository.findById(googleId)
            .orElse(null);
        if (googleUser == null) {
            // 구글 사용자 email 동일한 email 가진 회원이 있는지 확인
            String googleEmail = googleUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(googleEmail).orElse(null);
            if (sameEmailUser != null) {
                googleUser = sameEmailUser;
                // 기존 회원정보에 카카오 Id 추가
                googleUser = googleUser.googleIdUpdate(googleId.toString());
            } else {
                // 신규 회원가입
                // password: random UUID
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);

                // email: kakao email
                String email = googleUserInfo.getEmail();

                googleUser = new User(encodedPassword, email, googleUserInfo.getNickname(), UserRoleEnum.USER, googleId.toString());
            }

            userRepository.save(googleUser);
        }
    }


}