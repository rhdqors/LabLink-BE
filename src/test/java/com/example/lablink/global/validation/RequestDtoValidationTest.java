package com.example.lablink.global.validation;

import com.example.lablink.domain.application.dto.Request.ApplicationRequestDto;
import com.example.lablink.domain.application.dto.Request.ApplicationStatusRequestDto;
import com.example.lablink.domain.feedback.dto.Request.FeedBackRequestDto;
import com.example.lablink.domain.study.dto.requestDto.StudyRequestDto;
import com.example.lablink.domain.study.entity.CategoryEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequestDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Nested
    @DisplayName("StudyRequestDto 검증")
    class StudyRequestDtoTest {

        @Test
        @DisplayName("성공 - 모든 필수 필드 입력")
        void validStudyRequestDto() {
            StudyRequestDto dto = StudyRequestDto.builder()
                    .title("실험 제목")
                    .studyInfo("실험 정보")
                    .description("상세 설명")
                    .category(CategoryEnum.ONLINE)
                    .date(LocalDateTime.now().plusDays(7))
                    .address("서울시 강남구")
                    .pay(50000)
                    .subjectGender("남성")
                    .subjectMinAge(20)
                    .subjectMaxAge(30)
                    .endDate(LocalDateTime.now().plusDays(5))
                    .build();

            Set<ConstraintViolation<StudyRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        void invalidStudyRequestDto_missingFields() {
            StudyRequestDto dto = StudyRequestDto.builder().build();

            Set<ConstraintViolation<StudyRequestDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            // title, studyInfo, description, category, date, address, pay, subjectGender, subjectMinAge, subjectMaxAge, endDate
            assertEquals(11, violations.size());
        }

        @Test
        @DisplayName("실패 - pay 음수")
        void invalidStudyRequestDto_negativePay() {
            StudyRequestDto dto = StudyRequestDto.builder()
                    .title("실험 제목")
                    .studyInfo("실험 정보")
                    .description("상세 설명")
                    .category(CategoryEnum.ONLINE)
                    .date(LocalDateTime.now().plusDays(7))
                    .address("서울시 강남구")
                    .pay(-1)
                    .subjectGender("남성")
                    .subjectMinAge(20)
                    .subjectMaxAge(30)
                    .endDate(LocalDateTime.now().plusDays(5))
                    .build();

            Set<ConstraintViolation<StudyRequestDto>> violations = validator.validate(dto);
            assertEquals(1, violations.size());
            assertTrue(violations.iterator().next().getMessage().contains("0 이상"));
        }
    }

    @Nested
    @DisplayName("ApplicationRequestDto 검증")
    class ApplicationRequestDtoTest {

        private ApplicationRequestDto createValidDto() {
            ApplicationRequestDto dto = new ApplicationRequestDto();
            dto.setMessage("참여 희망합니다.");
            dto.setUserName("홍길동");
            dto.setUserPhone("01012345678");
            dto.setUserAddress("서울시 강남구");
            dto.setUserDetailAddress("101동 201호");
            dto.setUserGender("남성");
            dto.setDateOfBirth(LocalDate.of(1995, 1, 1));
            return dto;
        }

        @Test
        @DisplayName("성공 - 모든 필수 필드 입력")
        void validApplicationRequestDto() {
            ApplicationRequestDto dto = createValidDto();

            Set<ConstraintViolation<ApplicationRequestDto>> violations = validator.validate(dto);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        void invalidApplicationRequestDto_missingFields() {
            ApplicationRequestDto dto = new ApplicationRequestDto();

            Set<ConstraintViolation<ApplicationRequestDto>> violations = validator.validate(dto);
            assertFalse(violations.isEmpty());
            // message, userName, userPhone, userAddress, userDetailAddress, userGender, dateOfBirth
            assertEquals(7, violations.size());
        }

        @Test
        @DisplayName("실패 - 전화번호 형식 오류")
        void invalidApplicationRequestDto_invalidPhone() {
            ApplicationRequestDto dto = createValidDto();
            dto.setUserPhone("010-1234-5678");

            Set<ConstraintViolation<ApplicationRequestDto>> violations = validator.validate(dto);
            assertEquals(1, violations.size());
            assertTrue(violations.iterator().next().getMessage().contains("숫자만"));
        }
    }

    @Nested
    @DisplayName("ApplicationStatusRequestDto 검증")
    class ApplicationStatusRequestDtoTest {

        @Test
        @DisplayName("실패 - approvalStatus 빈 값")
        void invalidApplicationStatusRequestDto_blank() {
            ApplicationStatusRequestDto dto = new ApplicationStatusRequestDto();

            Set<ConstraintViolation<ApplicationStatusRequestDto>> violations = validator.validate(dto);
            assertEquals(1, violations.size());
        }
    }

    @Nested
    @DisplayName("FeedBackRequestDto 검증")
    class FeedBackRequestDtoTest {

        @Test
        @DisplayName("실패 - feedbackMessage 빈 값")
        void invalidFeedBackRequestDto_blank() {
            FeedBackRequestDto dto = new FeedBackRequestDto();

            Set<ConstraintViolation<FeedBackRequestDto>> violations = validator.validate(dto);
            assertEquals(1, violations.size());
        }
    }
}
