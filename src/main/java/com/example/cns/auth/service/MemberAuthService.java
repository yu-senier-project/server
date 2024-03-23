package com.example.cns.auth.service;

import com.example.cns.auth.domain.RefreshToken;
import com.example.cns.auth.dto.AuthTokens;
import com.example.cns.auth.dto.LoginReq;
import com.example.cns.auth.dto.SignUpReq;
import com.example.cns.common.exception.ExceptionCode;
import com.example.cns.common.security.exception.AuthException;
import com.example.cns.common.security.jwt.dto.JwtUserInfo;
import com.example.cns.common.security.jwt.provider.JwtProvider;
import com.example.cns.company.domain.Company;
import com.example.cns.company.service.CompanySearchService;
import com.example.cns.member.domain.Member;
import com.example.cns.member.service.MemberService;
import com.example.cns.member.type.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberAuthService {
    private final MemberService memberService;
    private final CompanySearchService companySearchService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    public void register(SignUpReq dto) {
        Member requestMember = dto.toEntity(passwordEncoder);
        validateMember(requestMember);

        if (requestMember.getRole() == RoleType.EMPLOYEE) {
            Company company = companySearchService.findByCompanyName(dto.companyName());
            requestMember.enrollCompany(company);
        }

        memberService.saveMember(requestMember);
    }

    public AuthTokens login(LoginReq dto) {
        Member member = memberService.findByUserName(dto.username());

        if (passwordEncoder.matches(dto.password(), member.getPassword())) {
            JwtUserInfo userInfo = new JwtUserInfo(member.getId(), member.getRole());
            AuthTokens authTokens = jwtProvider.generateLoginToken(userInfo);
            refreshTokenService.saveRefreshToken(new RefreshToken(authTokens.refreshToken(), userInfo));

            return authTokens;
        }
        throw new AuthException(ExceptionCode.INVALID_PASSWORD);
    }

    public boolean checkDuplicateUsername(String username) {
        return memberService.isExistByUsername(username);
    }

    public AuthTokens refresh(final String refreshTokenReq) {
        if (!jwtProvider.isTokenExpired(refreshTokenReq)) {
            RefreshToken refreshToken = refreshTokenService.findById(refreshTokenReq);

            return jwtProvider.generateLoginToken(refreshToken.getUserInfo());
        }
        throw new AuthException(ExceptionCode.EXPIRED_TOKEN);
    }

    private void validateMember(Member member) {
        if (memberService.isExistByEmail(member.getEmail()))
            throw new AuthException(ExceptionCode.DUPLICATE_EMAIL_EXISTS);
        if (memberService.isExistByUsername(member.getUsername()))
            throw new AuthException(ExceptionCode.DUPLICATE_USERNAME_EXISTS);
    }
}
