package com.example.lablink.global.auth;

import com.example.lablink.domain.company.repository.CompanyRepository;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 검증 서비스
 * User와 Company 간 순환 참조 해결을 위해 분리된 서비스
 */
@Service
@RequiredArgsConstructor
public class EmailValidationService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    /**
     * User 테이블에 이메일 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsInUser(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Company 테이블에 이메일 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsInCompany(String email) {
        return companyRepository.existsByEmail(email);
    }

    /**
     * User 또는 Company 테이블에 이메일 존재 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean existsInAny(String email) {
        return existsInUser(email) || existsInCompany(email);
    }

    /**
     * 이메일 중복 검증 (User + Company)
     * 중복 시 GlobalException 발생
     */
    @Transactional(readOnly = true)
    public void validateEmailNotDuplicated(String email) {
        if (existsInAny(email)) {
            throw new GlobalException(GlobalErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * User 테이블 기준 이메일 중복 검증
     * 중복 시 GlobalException 발생
     */
    @Transactional(readOnly = true)
    public void validateUserEmailNotDuplicated(String email) {
        if (existsInUser(email)) {
            throw new GlobalException(GlobalErrorCode.DUPLICATE_EMAIL);
        }
    }

    /**
     * Company 테이블 기준 이메일 중복 검증
     * 중복 시 GlobalException 발생
     */
    @Transactional(readOnly = true)
    public void validateCompanyEmailNotDuplicated(String email) {
        if (existsInCompany(email)) {
            throw new GlobalException(GlobalErrorCode.DUPLICATE_EMAIL);
        }
    }
}
