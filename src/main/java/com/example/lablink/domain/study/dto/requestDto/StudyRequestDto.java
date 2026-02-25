package com.example.lablink.domain.study.dto.requestDto;
import com.example.lablink.domain.study.entity.CategoryEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class StudyRequestDto {
    @NotBlank(message = "제목을 입력해 주세요.")
    private String title;

    @NotBlank(message = "공고 정보를 입력해 주세요.")
    private String studyInfo;

    @NotBlank(message = "상세 설명을 입력해 주세요.")
    private String description;

    private String benefit; // 우대사항

    @NotNull(message = "카테고리를 선택해 주세요.")
    private CategoryEnum category;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    @NotNull(message = "실험 일시를 입력해 주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime date;

    @NotBlank(message = "주소를 입력해 주세요.")
    private String address;

    @NotNull(message = "보수를 입력해 주세요.")
    @Min(value = 0, message = "보수는 0 이상이어야 합니다.")
    private Integer pay;

    @NotBlank(message = "대상 성별을 입력해 주세요.")
    private String subjectGender;

    @NotNull(message = "최소 나이를 입력해 주세요.")
    @Min(value = 0, message = "최소 나이는 0 이상이어야 합니다.")
    private Integer subjectMinAge;

    @NotNull(message = "최대 나이를 입력해 주세요.")
    @Min(value = 0, message = "최대 나이는 0 이상이어야 합니다.")
    private Integer subjectMaxAge;

//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    @NotNull(message = "마감 일시를 입력해 주세요.")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endDate;

    MultipartFile thumbnailImage;
    MultipartFile detailImage;
}
