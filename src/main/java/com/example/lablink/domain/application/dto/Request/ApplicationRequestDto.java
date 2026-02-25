package com.example.lablink.domain.application.dto.Request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Getter
@Setter
public class ApplicationRequestDto {
    @NotBlank(message = "신청 메시지를 입력해 주세요.")
    private String message;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String userName;

    @NotBlank(message = "전화번호를 입력해 주세요.")
    @Pattern(regexp = "^\\d{1,11}$", message = "'-'를 제외한 숫자만 입력해주세요.")
    private String userPhone;

    @NotBlank(message = "주소를 입력해 주세요.")
    private String userAddress;

    @NotBlank(message = "상세 주소를 입력해 주세요.")
    private String userDetailAddress;

    @NotBlank(message = "성별을 입력해 주세요.")
    private String userGender;

    @NotNull(message = "생년월일을 입력해 주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

}
