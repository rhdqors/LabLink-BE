package com.example.lablink.domain.user.entity;

import com.example.lablink.global.timestamp.entity.Timestamped;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.time.LocalDate;

@Entity(name = "users")
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@Where(clause = "deleted_at IS NULL")
@SQLDelete(sql = "UPDATE users SET deleted_at = CONVERT_TZ(now(), 'UTC', 'Asia/Seoul') WHERE id = ?")
public class User extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickName;

    // xxx :  카카오 로그인은 password 없으니까 .. ?
    @Column(nullable = true)
    private String password;

    @Column(nullable = true)
    private String userName;

    @Column(nullable = true)
    private LocalDate dateOfBirth;

    @Column(nullable = true)
    private String userGender;

    // xxx : kakaoid 추가했슴니다 ..
    @Column(nullable = true, unique = true)
    private Long kakaoId;

    @Column(unique = true)
    private String kakaoEmail;

    @Column(unique = true)
    private String naverEmail;

    @Column(unique = true)
    private String googleEmail;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private UserRoleEnum role;

    @OneToOne
    @JoinColumn(name = "userinfo_id", nullable = false)
    private UserInfo userinfo;

    public User(String email, String nickName, String password, UserInfo userinfo, UserRoleEnum role) {
        this.email = email;
        this.nickName = nickName;
        this.password = password;
        this.userinfo = userinfo;
        this.role = role;
    }

    // 카카오 로그인시
    public User(Long kakaoId, String nickname, String email, UserInfo userInfo, UserRoleEnum role){
        this.kakaoId = kakaoId;
        this.nickName = nickname;
        if(email != null) this.kakaoEmail = email;
        this.userinfo = userInfo;
        this.role = role;
    }

    public void updateUser(String userName, LocalDate dateOfBirth, String userGender) {
        this.userName = userName;
        this.dateOfBirth = dateOfBirth;
        this.userGender = userGender;
    }

    public User googleIdUpdate(String googleEmail) {
        this.googleEmail = googleEmail;
        return this;
    }

    public User(String username, String password, String nickname, UserRoleEnum role, String googleEmail) {
        this.userName = username;
        this.password = password;
        this.nickName = nickname;
        this.role = role;
        this.googleEmail = googleEmail;
    }

}
