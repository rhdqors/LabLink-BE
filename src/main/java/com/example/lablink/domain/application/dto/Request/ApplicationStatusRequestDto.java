package com.example.lablink.domain.application.dto.Request;

import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class ApplicationStatusRequestDto {
    @NotBlank(message = "승인 상태를 입력해 주세요.")
    private String approvalStatus;
}
