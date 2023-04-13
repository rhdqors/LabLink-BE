package com.example.lablink.company.security;

import com.example.lablink.company.entity.Company;
import com.example.lablink.user.entity.UserRoleEnum;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

/* 로그인시 인증된 사용자의 정보를 갖고있는 클래스
* UserDetailsService에서 반환하는 User 객체, 파라미터를 받아 생성됨*/
public class CompanyDetailsImpl implements UserDetails {

    private final Company company;
    private final String email;

    public CompanyDetailsImpl(Company company, String email) {
        this.company = company;
        this.email = email;
    }

    public Company getCompany() {
        return company;
    }

//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return Collections.emptyList();
//    }

    // 인증 객체 생성 이전에 로그인한 user or company에게 권한 설정
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        UserRoleEnum companyRole = company.getRole(); // 기업 권한 정보
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // 기업 엔티티에 대한 권한 추가
        SimpleGrantedAuthority companyAuthority = new SimpleGrantedAuthority(companyRole.getAuthority());
        authorities.add(companyAuthority);

        return authorities;
    }

    // 인증에 사용될 이름(이메일)
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}