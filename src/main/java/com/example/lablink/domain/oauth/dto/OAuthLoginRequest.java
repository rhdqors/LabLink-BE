package com.example.lablink.domain.oauth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginRequest {
    @NotBlank
    private String code;
    private String state;
}
