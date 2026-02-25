package com.example.lablink.domain.user.service;

import com.example.lablink.domain.user.repository.TermsRepository;
import com.example.lablink.domain.user.dto.request.SignupRequestDto;
import com.example.lablink.domain.user.dto.request.TermsRequestDto;
import com.example.lablink.domain.user.entity.Terms;
import com.example.lablink.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;

    public Terms saveTerms(SignupRequestDto signupRequestDto, User user) {
        return termsRepository.save(new Terms(
            signupRequestDto.isAgeCheck(),
            signupRequestDto.isTermsOfServiceAgreement(),
            signupRequestDto.isPrivacyPolicyConsent(),
            signupRequestDto.isSensitiveInfoConsent(),
            signupRequestDto.isMarketingOptIn(),
            user
        ));
    }

    public Terms saveSocialTerms(TermsRequestDto termsRequestDto, User user) {
        return termsRepository.save(new Terms(
            termsRequestDto.isAgeCheck(),
            termsRequestDto.isTermsOfServiceAgreement(),
            termsRequestDto.isPrivacyPolicyConsent(),
            termsRequestDto.isSensitiveInfoConsent(),
            termsRequestDto.isMarketingOptIn(),
            user
        ));
    }

    public void deleteTerms(User user) {
        termsRepository.deleteByUser(user);
    }
}
