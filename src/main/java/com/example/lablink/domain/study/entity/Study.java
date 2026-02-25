package com.example.lablink.domain.study.entity;

import com.example.lablink.domain.company.entity.Company;
import com.example.lablink.global.common.constants.ImageConstants;
import com.example.lablink.global.timestamp.entity.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
@SQLDelete(sql = "UPDATE study SET deleted_at = CONVERT_TZ(now(), 'UTC', 'Asia/Seoul') WHERE id = ?")
public class Study extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String studyInfo;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = true)
    private String benefit;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private int pay;

    @Column(nullable = false)
    private String subjectGender;

    @Column(nullable = false)
    private int subjectMinAge;

    @Column(nullable = false)
    private int subjectMaxAge;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private String thumbnailImageURL;

    @Column(nullable = false)
    private String detailImageURL;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private StudyStatusEnum status;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CategoryEnum category;

    @Column(nullable = false)
    private boolean emailSend;

    @Column(nullable = false)
    private int currentApplicantCount; // 지원자 현황

    public Study(String title, String studyInfo, String description, String benefit,
                 LocalDateTime date, String address, int pay, String subjectGender,
                 int subjectMinAge, int subjectMaxAge, LocalDateTime endDate,
                 CategoryEnum category, StudyStatusEnum status, Company company,
                 String thumbnailImageURL, String detailImageURL) {
        this.title = title;
        this.studyInfo = studyInfo;
        this.description = description;
        this.benefit = benefit;
        this.date = date;
        this.address = address;
        this.pay = pay;
        this.subjectGender = subjectGender;
        this.subjectMinAge = subjectMinAge;
        this.subjectMaxAge = subjectMaxAge;
        this.endDate = endDate;
        this.category = category;
        this.status = status;
        this.company = company;
        this.thumbnailImageURL = Objects.requireNonNullElse(thumbnailImageURL, ImageConstants.DEFAULT_IMAGE_URL);
        this.detailImageURL = Objects.requireNonNullElse(detailImageURL, ImageConstants.DEFAULT_IMAGE_URL);
        this.emailSend = false;
        this.currentApplicantCount = 0;
    }

    public void update(StudyStatusEnum status, String thumbnailImageURL, String detailImageURL) {
        if (thumbnailImageURL != null) this.thumbnailImageURL = thumbnailImageURL;
        if (detailImageURL != null) this.detailImageURL = detailImageURL;
        this.status = status;
    }

    public void updateStatus(StudyStatusEnum status) {
        this.status = status;
    }

    public void updateEmailSend() {
        this.emailSend = true;
    }

    public void updateCurrentApplicantCount() {
        ++this.currentApplicantCount;
    }

    public void deleteThumbnail(){
        this.thumbnailImageURL = ImageConstants.DEFAULT_IMAGE_URL;
    }

    public void deleteDetailImage(){
        this.detailImageURL = ImageConstants.DEFAULT_IMAGE_URL;
    }
}
