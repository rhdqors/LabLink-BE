package com.example.lablink.domain.user.service;

import com.example.lablink.domain.user.dto.request.UserNickNameRequestDto;
import com.example.lablink.domain.auth.service.AuthService;
import com.example.lablink.domain.user.entity.User;
import com.example.lablink.domain.user.entity.UserRoleEnum;
import com.example.lablink.domain.user.repository.UserQueryRepository;
import com.example.lablink.domain.user.repository.UserRepository;
import com.example.lablink.domain.user.security.UserDetailsImpl;
import com.example.lablink.global.auth.EmailValidationService;
import com.example.lablink.global.common.dto.request.SignupEmailCheckRequestDto;
import com.example.lablink.global.exception.GlobalErrorCode;
import com.example.lablink.global.exception.GlobalException;
import com.example.lablink.global.jwt.JwtUtil;
import com.example.lablink.global.util.CookieUtil;

import com.example.lablink.domain.user.dto.request.LoginRequestDto;
import com.example.lablink.domain.user.dto.request.SignupRequestDto;
import com.example.lablink.domain.user.dto.response.MyLabResponseDto;

import com.example.lablink.domain.user.entity.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(UserService.class); // 메서드 실행 시간 측정 ex) 회원탈퇴
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TermsService termsService;
    private final JwtUtil jwtUtil;
    private final UserInfoService userInfoService;
    private final EntityManager em;
    private final EmailValidationService emailValidationService;
    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final UserQueryRepository userQueryRepository;

    // 유저 회원가입
    @Transactional
    public String signup(SignupRequestDto signupRequestDto) {
        String email = signupRequestDto.getEmail();
        String password = passwordEncoder.encode(signupRequestDto.getPassword());

        // 이메일 중복 확인 (User + Company 테이블)
        emailValidationService.validateEmailNotDuplicated(email);

        // 가입 닉네임 중복 확인
        if (userRepository.existsByNickName(signupRequestDto.getNickName())) {
            throw new GlobalException(GlobalErrorCode.DUPLICATE_NICK_NAME);
        }

        // 필수 약관 동의
        if(!signupRequestDto.isAgeCheck() || !signupRequestDto.isTermsOfServiceAgreement() || !signupRequestDto.isPrivacyPolicyConsent() || !signupRequestDto.isSensitiveInfoConsent()) {
            throw new GlobalException(GlobalErrorCode.NEED_AGREE_REQUIRE_TERMS);
        }
        // 유저 저장 및 유저를 약관에 저장시킴 -> 약관을 유저에 저장시키면 유저를 불러올때마다 약관이 불려와 무거움
        // userinfo는 회원가입할 때 받지 않음.
        UserInfo userInfo = userInfoService.saveUserInfo(signupRequestDto);
        User user = userRepository.save(new User(
            signupRequestDto.getEmail(),
            signupRequestDto.getNickName(),
            password,
            userInfo,
            UserRoleEnum.USER
        ));
        termsService.saveTerms(signupRequestDto, user);
        return "회원가입 완료.";
    }

    // 유저 로그인
    @Transactional
    public String login(LoginRequestDto loginRequestDto, HttpServletResponse response) {
        String email = loginRequestDto.getEmail();
        String password = loginRequestDto.getPassword();

        // 이메일 존재, 일치 여부
        User user = userRepository.findByEmail(email).orElseThrow(() -> new GlobalException(GlobalErrorCode.EMAIL_NOT_FOUND));

        // 비밀번호 일치 여부
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new GlobalException(GlobalErrorCode.PASSWORD_MISMATCH);
        }

        // Access token 생성 및 헤더에 추가
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createUserToken(user));

        // Refresh token 생성 및 쿠키에 저장
        String refreshToken = authService.generateAndStoreRefreshToken(user.getId(), UserRoleEnum.USER);
        cookieUtil.addRefreshTokenCookie(response, refreshToken, JwtUtil.RF_TOKEN_TIME / 1000);

        return "로그인 완료.";
    }

    // 유저 이메일 중복 체크
    @Transactional(readOnly = true)
    public String emailCheck(SignupEmailCheckRequestDto signupEmailCheckRequestDto) {
        // User + Company 테이블 이메일 중복 확인
        emailValidationService.validateEmailNotDuplicated(signupEmailCheckRequestDto.getEmail());
        return "사용 가능합니다.";
    }

    // 유저 닉네임 중복 확인
    public String nickNameCheck(UserNickNameRequestDto userNickNameRequestDto) {
        if(userRepository.existsByNickName(userNickNameRequestDto.getNickName())) {
            throw new GlobalException(GlobalErrorCode.DUPLICATE_NICK_NAME);
        }
        return "사용 가능합니다.";
    }

    // 인증 유저 가져오기
    public User getUser(UserDetailsImpl userDetails){
        return  userRepository.findById(userDetails.getUser().getId()).orElseThrow(
            ()->new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
    }

    // 회원 탈퇴
    @Transactional
    public String deleteUser(UserDetailsImpl userDetails, HttpServletResponse response) {
        long start = System.currentTimeMillis();

        User user = userDetails.getUser();
        // RT revoke + 쿠키 클리어
        authService.logout(user.getId(), UserRoleEnum.USER, response);
        // 삭제 & AT 무효화
        userRepository.deleteUserAndData(user.getId());
        response.setHeader(JwtUtil.AUTHORIZATION_HEADER, null);

        long end = System.currentTimeMillis();
        logger.info("deleteUser took {} ms", end - start);
        return "탈퇴 완료.";
    }

    // 내 실험 관리 - 신청한 목록
    @Transactional
    @Cacheable(value = "myLab", key = "#userDetails.getUser().getId()")
    public List<MyLabResponseDto> getMyLabs(UserDetailsImpl userDetails) {
        long start = System.currentTimeMillis();

        if(userDetails == null || userDetails.equals(" ")) {
            throw new GlobalException(GlobalErrorCode.INVALID_TOKEN);
        }
            // JPA
//        List<Application> applications = applicationService.findAllByMyApplication(userDetails.getUser());
//        List<MyLabResponseDto> myLabs = new ArrayList<>();
//
//        for (Application application : applications) {
//            Study study = getStudyService.getStudy(application.getStudyId());
//            myLabs.add(new MyLabResponseDto(study.getId(),
//                                            study.getTitle(),
//                                            study.getCreatedAt(),
//                                            study.getPay(),
//                                            study.getAddress(),
//                                            application.getApplicationViewStatusEnum(),
//                                            application.getApprovalStatusEnum(),
//                                            study.getDate(),
//                                            study.getCompany().getCompanyName()));
//        }
//        long end = System.currentTimeMillis();
//        logger.info("getMyLabs took {} ms", end - start);
//        return myLabs;

        // 내가 신청한 목록
        TypedQuery<MyLabResponseDto> query = em.createQuery(
                "SELECT new com.example.lablink.domain.user.dto.response.MyLabResponseDto(s.id, s.title, s.createdAt, s.pay, s.address, a.applicationViewStatusEnum, a.approvalStatusEnum, s.date, s.company.companyName) " +
                        "FROM Study s LEFT JOIN Application a ON s.id = a.studyId WHERE a.user.id = :userId", MyLabResponseDto.class);
        query.setParameter("userId", userDetails.getUser().getId());

        long end = System.currentTimeMillis();
        logger.info("getMyLabs took {} ms", end - start);

        //QueryDSL
//        return userQueryRepository.getMyLabResponseDto(userDetails.getUser());
        return query.getResultList();
    }


    public User getUserByNickname(String nickName) {
        return userRepository.findByNickName(nickName).orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));
    }

}