package com.example.lablink.global.exception;

import com.example.lablink.global.message.ResponseMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {

        @Test
        @DisplayName("GlobalException 처리 시 해당 에러 코드의 상태와 메시지를 반환한다")
        void handleGlobalException() {
            // given
            GlobalException exception = new GlobalException(GlobalErrorCode.USER_NOT_FOUND);

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleCustomException(exception);

            // then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(GlobalErrorCode.USER_NOT_FOUND.getMessage(), response.getBody().getMessage());
        }

        @Test
        @DisplayName("cause가 포함된 GlobalException 처리 시 에러 코드를 정상 반환한다")
        void handleGlobalExceptionWithCause() {
            // given
            RuntimeException cause = new RuntimeException("원인 예외");
            GlobalException exception = new GlobalException(GlobalErrorCode.USER_NOT_FOUND, cause);

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleCustomException(exception);

            // then
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals(cause, exception.getCause());
        }
    }

    @Nested
    @DisplayName("검증 실패 처리")
    class ValidationCase {

        @Test
        @DisplayName("MethodArgumentNotValidException 처리 시 400 상태와 필드별 에러 메시지를 반환한다")
        void handleMethodArgumentNotValidException() throws NoSuchMethodException {
            // given
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
            bindingResult.addError(new FieldError("testObject", "title", "제목은 필수입니다"));
            bindingResult.addError(new FieldError("testObject", "pay", "급여는 0 이상이어야 합니다"));

            MethodParameter methodParameter = new MethodParameter(
                    this.getClass().getDeclaredMethod("handleMethodArgumentNotValidException"), -1
            );
            MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleValidationException(exception);

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
            // 필드별 에러 메시지가 data에 포함되어야 한다
            assertNotNull(response.getBody().getData());
        }

        @Test
        @DisplayName("MethodArgumentTypeMismatchException 처리 시 400 상태를 반환한다")
        void handleMethodArgumentTypeMismatchException() {
            // given
            MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "studyId", null, new NumberFormatException("For input string: \"abc\"")
            );

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleTypeMismatchException(exception);

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }

        @Test
        @DisplayName("MissingServletRequestParameterException 처리 시 400 상태를 반환한다")
        void handleMissingServletRequestParameterException() {
            // given
            MissingServletRequestParameterException exception =
                    new MissingServletRequestParameterException("pageIndex", "int");

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleMissingParameterException(exception);

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(400, response.getBody().getStatusCode());
        }
    }

    @Nested
    @DisplayName("HTTP 메서드 오류 처리")
    class HttpMethodCase {

        @Test
        @DisplayName("HttpRequestMethodNotSupportedException 처리 시 405 상태를 반환한다")
        void handleHttpRequestMethodNotSupportedException() {
            // given
            HttpRequestMethodNotSupportedException exception =
                    new HttpRequestMethodNotSupportedException("DELETE");

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleMethodNotSupportedException(exception);

            // then
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(405, response.getBody().getStatusCode());
        }
    }

    @Nested
    @DisplayName("예상치 못한 예외 처리")
    class UnexpectedExceptionCase {

        @Test
        @DisplayName("처리되지 않은 Exception 발생 시 500 상태를 반환하고 내부 정보를 은닉한다")
        void handleUnexpectedException() {
            // given
            Exception exception = new RuntimeException("DB connection failed");

            // when
            ResponseEntity<ResponseMessage> response = globalExceptionHandler.handleUnexpectedException(exception);

            // then
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(500, response.getBody().getStatusCode());
            // 내부 에러 메시지가 그대로 노출되지 않아야 한다
            assertFalse(response.getBody().getMessage().contains("DB connection failed"));
        }
    }
}
