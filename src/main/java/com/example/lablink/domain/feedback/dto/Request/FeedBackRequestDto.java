package com.example.lablink.domain.feedback.dto.Request;

import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
public class FeedBackRequestDto {
    @NotBlank(message = "피드백 내용을 입력해 주세요.")
    private String feedbackMessage;
}
