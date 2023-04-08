package com.example.lablink.user.dto.response;

import com.example.lablink.message.ResponseMessage;
import lombok.Getter;

@Getter
public class UserEmailCheckResponseDto <T> extends ResponseMessage {

    private int status;
    private String message;
    private T data;

    public UserEmailCheckResponseDto(int statusCode, String message, T data) {
        super(statusCode, message, data);
    }

}
