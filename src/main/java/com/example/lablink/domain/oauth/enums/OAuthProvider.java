package com.example.lablink.domain.oauth.enums;

import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;

public enum OAuthProvider {
    KAKAO, GOOGLE;

    public static OAuthProvider from(String value) {
        try {
            return OAuthProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GlobalException(GlobalErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
