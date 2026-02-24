package com.example.lablink.global.exception;

import com.example.lablink.global.message.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = { GlobalException.class })
    protected ResponseEntity<ResponseMessage> handleCustomException(GlobalException e) {
        log.error("GlobalException: errorCode={}", e.getErrorCode(), e);
        return ResponseMessage.ErrorResponse(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ResponseMessage> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "유효하지 않은 값입니다",
                        (existing, replacement) -> existing
                ));
        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.<Map<String, String>>builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message(GlobalErrorCode.INVALID_INPUT_VALUE.getMessage())
                        .data(fieldErrors)
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ResponseMessage> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        log.warn("Type mismatch: parameter={}, value={}", paramName, e.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("파라미터 '" + paramName + "'의 타입이 올바르지 않습니다")
                        .data("")
                        .build()
                );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    protected ResponseEntity<ResponseMessage> handleMissingParameterException(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: name={}, type={}", e.getParameterName(), e.getParameterType());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseMessage.builder()
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("필수 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다")
                        .data("")
                        .build()
                );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ResponseMessage> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: method={}", e.getMethod());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseMessage.builder()
                        .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .message(GlobalErrorCode.METHOD_NOT_ALLOWED.getMessage())
                        .data("")
                        .build()
                );
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ResponseMessage> handleUnexpectedException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseMessage.builder()
                        .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message(GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                        .data("")
                        .build()
                );
    }
}
