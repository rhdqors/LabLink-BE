package com.example.lablink.domain.company.entity;

import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.global.common.constants.ImageConstants;
import com.example.lablink.global.timestamp.entity.Timestamped;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Getter
@Setter
@NoArgsConstructor
//@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@SQLDelete(sql = "UPDATE company SET deleted_at = CONVERT_TZ(now(), 'UTC', 'Asia/Seoul') WHERE id = ?")
public class Company extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String companyName;

    @Column(nullable = false)
    private String ownerName;

    @Column(nullable = false)
    private String business;

    @Column(nullable = false)
    private String managerPhone;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String detailAddress;

    @Column(nullable = false)
    private String logoUrl;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserRoleEnum role;

    public Company(String email, String password, String companyName, String ownerName,
                   String business, String managerPhone, String address, String detailAddress,
                   String logoUrl, UserRoleEnum role) {
        this.email = email;
        this.password = password;
        this.companyName = companyName;
        this.ownerName = ownerName;
        this.business = business;
        this.managerPhone = managerPhone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.logoUrl = Objects.requireNonNullElse(logoUrl, ImageConstants.DEFAULT_IMAGE_URL);
        this.role = role;
    }

    @Builder
    public Company(Long id, String email, String password, String companyName, String ownerName, String business, String managerPhone, String address, String detailAddress, UserRoleEnum role){
        this.id = id;
        this.email = email;
        this.password = password;
        this.companyName = companyName;
        this.ownerName = ownerName;
        this.business = business;
        this.managerPhone = managerPhone;
        this.address = address;
        this.detailAddress = detailAddress;
        this.role = role;
    }

}
