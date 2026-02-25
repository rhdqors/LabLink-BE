package com.example.lablink.domain.user.service;

import com.example.lablink.domain.user.entity.UserInfo;
import com.example.lablink.domain.user.repository.UserInfoRepository;
import com.example.lablink.domain.user.dto.request.SignupRequestDto;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;

    public UserInfo saveUserInfo(SignupRequestDto signupRequestDto) {
        return userInfoRepository.save(new UserInfo(signupRequestDto.getUserPhone()));
    }

    public UserInfo saveKakaoUserInfo() {
        return userInfoRepository.save(new UserInfo());
    }

    public UserInfo findUserInfo(Long userInfoId) {
        return userInfoRepository.findById(userInfoId).orElseThrow(() -> new GlobalException(GlobalErrorCode.USERINFO_NOT_FOUND));
    }
}
