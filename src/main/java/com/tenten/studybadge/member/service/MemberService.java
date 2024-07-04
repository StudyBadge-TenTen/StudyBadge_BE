package com.tenten.studybadge.member.service;

import com.tenten.studybadge.common.email.MailService;
import com.tenten.studybadge.common.exception.InvalidTokenException;
import com.tenten.studybadge.common.exception.member.*;
import com.tenten.studybadge.common.jwt.JwtTokenProvider;
import com.tenten.studybadge.common.redis.RedisService;
import com.tenten.studybadge.member.dto.MemberLoginRequest;
import com.tenten.studybadge.member.dto.MemberSignUpRequest;
import com.tenten.studybadge.member.domain.entity.Member;
import com.tenten.studybadge.member.domain.repository.MemberRepository;
import com.tenten.studybadge.member.dto.TokenCreateDto;
import com.tenten.studybadge.type.member.MemberStatus;
import com.tenten.studybadge.type.member.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MailService mailService;
    private final RedisService redisService;
    private final RedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Transactional
    public void signUp(MemberSignUpRequest signUpRequest, Platform platform) {

        if(!mailService.isValidEmail(signUpRequest.getEmail())) {
            throw new InvalidEmailException();
        }

        if(!signUpRequest.getPassword().equals(signUpRequest.getCheckPassword())) {
            throw new NotMatchPasswordException();
        }

        Member member = null;

        Optional<Member> byEmail = memberRepository.findByEmailAndPlatform(signUpRequest.getEmail(), platform);
        if (byEmail.isPresent()) {
            member = byEmail.get();

          if (!member.getStatus().equals(MemberStatus.WITHDRAWN)) {
              throw new DuplicateEmailException();
          }
        } else {

            member = new Member();

        }

        memberRepository.save(MemberSignUpRequest.toEntity(member, signUpRequest));

        String authCode = null;
        authCode = redisService.generateAuthCode();
        redisService.saveAuthCode(signUpRequest.getEmail(), authCode);

        mailService.sendMail(signUpRequest, authCode);
    }

    public void auth(String email, String code, Platform platform) {

        Member member = memberRepository.findByEmailAndPlatform(email, platform)
                        .orElseThrow(NotFoundMemberException::new);

        if (member.getIsAuth()) {
            throw new AlreadyAuthException();
        }

        if (!code.equals(redisService.getAuthCode(email))) {
            throw new InvalidAuthCodeException();
        }

        Member authMember = member.toBuilder()
                .isAuth(true)
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(authMember);

        redisService.deleteAuthCode(email);
    }

    public TokenCreateDto login(MemberLoginRequest loginRequest, Platform platform) {

        Member member = memberRepository.findByEmailAndPlatform(loginRequest.getEmail(), platform)
                        .orElseThrow(NotFoundMemberException::new);

        switch (member.getStatus()) {
            case SUSPENDED -> throw new RuntimeException();
            case WAIT_FOR_APPROVAL -> throw new BeforeAuthMemberException();
            case WITHDRAWN -> throw new NotFoundMemberException();
        }

        if(!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new IncorrectPasswordException();
        }

        TokenCreateDto result = TokenCreateDto.builder()
                .email(member.getEmail())
                .build();

        return result;
    }

    public void logout(String accessToken) {

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException();
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        Platform platform = jwtTokenProvider.getPlatform(accessToken);

        String refreshToken = "RefreshToken: " + authentication.getName() + " : " + platform;
        if (redisTemplate.opsForValue()
                .get(refreshToken) != null) {

            redisTemplate.delete(refreshToken);
        }

        long expiration = jwtTokenProvider.getExpiration(accessToken);
        long now = new Date().getTime();

        long accessTokenExpiresIn = expiration - now;

        if(accessTokenExpiresIn > 0) {

            redisTemplate.opsForValue().set(
                    "logout: " + accessToken,
                    "logout",
                    accessTokenExpiresIn,
                    TimeUnit.MILLISECONDS);
        } else {
            throw new InvalidTokenException();
        }
    }
}


