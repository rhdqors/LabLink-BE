package com.example.lablink.domain.oauth.dto;

import com.example.lablink.domain.oauth.enums.OAuthProvider;

public record OAuthUserInfo(String providerId, String email, String nickname, OAuthProvider provider) {
}
