package com.example.lablink.domain.oauth.client;

import com.example.lablink.domain.oauth.dto.OAuthUserInfo;
import com.example.lablink.domain.oauth.enums.OAuthProvider;

public interface OAuthClient {
    OAuthProvider getProvider();
    String getAuthorizationUrl(String state);
    String getAccessToken(String code);
    OAuthUserInfo getUserInfo(String accessToken);
}
