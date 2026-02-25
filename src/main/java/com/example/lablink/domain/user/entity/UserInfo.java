package com.example.lablink.domain.user.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String userAddress;

    @Column(nullable = true)
    private String userDetailAddress;

    @Column(nullable = true)
    private String userPhone;

    public UserInfo(String userPhone) {
        this.userPhone = userPhone;
    }

    public void updateUserInfo(String userAddress, String userDetailAddress) {
        this.userAddress = userAddress;
        this.userDetailAddress = userDetailAddress;
    }
}
