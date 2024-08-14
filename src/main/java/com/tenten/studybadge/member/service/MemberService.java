package com.tenten.studybadge.member.service;

import com.tenten.studybadge.attendance.service.AttendanceService;
import com.tenten.studybadge.common.component.AwsS3Service;
import com.tenten.studybadge.common.email.MailService;
import com.tenten.studybadge.common.exception.InvalidTokenException;
import com.tenten.studybadge.common.exception.account.BeforeCertAccountException;
import com.tenten.studybadge.common.exception.member.*;
import com.tenten.studybadge.common.exception.participation.NotFoundParticipationException;
import com.tenten.studybadge.common.jwt.JwtTokenProvider;
import com.tenten.studybadge.common.redis.RedisService;
import com.tenten.studybadge.member.dto.*;
import com.tenten.studybadge.member.domain.entity.Member;
import com.tenten.studybadge.member.domain.repository.MemberRepository;
import com.tenten.studybadge.common.token.dto.TokenCreateDto;
import com.tenten.studybadge.participation.domain.entity.Participation;
import com.tenten.studybadge.participation.domain.repository.ParticipationRepository;
import com.tenten.studybadge.study.member.domain.entity.StudyMember;
import com.tenten.studybadge.study.member.domain.repository.StudyMemberRepository;
import com.tenten.studybadge.type.member.MemberStatus;
import com.tenten.studybadge.type.member.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;
import java.util.Optional;


import static com.tenten.studybadge.common.constant.TokenConstant.BEARER;
import static com.tenten.studybadge.common.constant.TokenConstant.REFRESH_TOKEN_FORMAT;
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MailService mailService;
    private final RedisService redisService;
    private final RedisTemplate redisTemplate;
    private final AwsS3Service awsS3Service;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StudyMemberRepository studyMemberRepository;
    private final ParticipationRepository participationRepository;
    private final AttendanceService attendanceService;

    public void signUp(MemberSignUpRequest signUpRequest, Platform platform) {

        if(!mailService.isValidEmail(signUpRequest.getEmail())) {
            throw new InvalidEmailException();
        }

        if(!signUpRequest.getPassword().equals(signUpRequest.getCheckPassword())) {
            throw new NotMatchPasswordException();
        }

        if (!signUpRequest.getIsAccountCert()) {
            throw new BeforeCertAccountException();
        }

        Optional<Member> byEmail = memberRepository.findByEmailAndPlatform(signUpRequest.getEmail(), platform);
        if (byEmail.isPresent()) {

            Member member = byEmail.get();

            if (!member.getStatus().equals(MemberStatus.WITHDRAWN)) {
                throw new DuplicateEmailException();
            }
        }

        Member member = byEmail.orElseGet(Member::new);

        memberRepository.save(MemberSignUpRequest.toEntity(member, signUpRequest));


        String authCode = redisService.generateAuthCode();
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

    public void reSendCode(String email, Platform platform) {

        memberRepository.findByEmailAndPlatform(email, platform)
                .orElseThrow(NotFoundMemberException::new);

        String authCode = redisService.generateAuthCode();
        redisService.saveAuthCode(email, authCode);

        mailService.reSendMail(email, authCode);
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

        return TokenCreateDto.builder()
                .id(String.valueOf(member.getId()))
                .email(member.getEmail())
                .role(member.getRole())
                .build();
    }

    public void logout(String accessToken) {

        if (accessToken.startsWith(BEARER)) {
            accessToken = accessToken.substring(7);
        } else {
            throw new InvalidTokenException();
        }

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new InvalidTokenException();
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        Platform platform = jwtTokenProvider.getPlatform(accessToken);

        String refreshToken = String.format(REFRESH_TOKEN_FORMAT, authentication.getName(), platform);
        if (redisTemplate.opsForValue().get(refreshToken) != null) {

            redisTemplate.delete(refreshToken);
        }

            redisService.blackList(accessToken);
    }

    public MemberResponse getMyInfo(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        return MemberResponse.toResponse(member);
    }

    public List<MemberStudyList> getMyStudy(Long memberId) {

        List<StudyMember> studyMembers = studyMemberRepository.findAllByMemberIdWithStudyChannel(memberId);
        if (studyMembers == null || studyMembers.isEmpty())

            throw new NotFoundMyStudyException();

        return MemberStudyList.listToResponse(studyMembers, attendanceService::getAttendanceRatioForMember);
    }

    public List<MemberApplyList> getMyApply(Long memberId) {

        List<Participation> participationList = participationRepository.findAllByMemberIdWithStudyChannel(memberId);
        if (participationList == null || participationList.isEmpty())

            throw new NotFoundParticipationException();

        return MemberApplyList.listToResponse(participationList);

    }

    public MemberResponse memberUpdate(Long memberId, MemberUpdateRequest updateRequest, MultipartFile profile) {


        if (profile != null && !profile.isEmpty()) {
            String imgUrl = awsS3Service.uploadFile(profile);
            updateRequest.setImgUrl(imgUrl);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        if ((member.getPlatform() == Platform.KAKAO || member.getPlatform() == Platform.NAVER)
                && member.getStatus().equals(MemberStatus.WAIT_FOR_APPROVAL)) {

            member = member.toBuilder()
                    .account(updateRequest.getAccount())
                    .accountBank(updateRequest.getAccountBank())
                    .nickname(updateRequest.getNickname())
                    .introduction(updateRequest.getIntroduction())
                    .imgUrl(updateRequest.getImgUrl())
                    .status(MemberStatus.ACTIVE)
                    .build();
        } else {

            member = member.toBuilder()
                    .account(updateRequest.getAccount())
                    .accountBank(updateRequest.getAccountBank())
                    .nickname(updateRequest.getNickname())
                    .introduction(updateRequest.getIntroduction())
                    .imgUrl(updateRequest.getImgUrl())
                    .build();
        }

        memberRepository.save(member);

        return MemberResponse.toResponse(member);
    }

    public void withdrawal(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        if (member.getPoint() > 0 ) {

            throw new ExistPointException();
        }

        String refreshToken = String.format(REFRESH_TOKEN_FORMAT, member.getId(), member.getPlatform());
        if (redisTemplate.opsForValue().get(refreshToken) != null) {

            redisTemplate.delete(refreshToken);
        }


        Member withdrawMember = member.toBuilder()
                .status(MemberStatus.WITHDRAWN)
                .build();
        memberRepository.save(withdrawMember);
    }

    public void requestReset(String email, Platform platform) {

        Member member = memberRepository.findByEmailAndPlatform(email, platform)
                .orElseThrow(NotFoundMemberException::new);

        String authCode = redisService.generateAuthCode();

        redisService.saveAuthCode(member.getEmail(), authCode);

        mailService.sendResetMail(member.getEmail() , authCode);
    }

    public void authPassword(String email, String code, Platform platform) {

        Member member = memberRepository.findByEmailAndPlatform(email, platform)
                .orElseThrow(NotFoundMemberException::new);

        if (!code.equals(redisService.getAuthCode(email))) {
            throw new InvalidAuthCodeException();
        }

        Member authResetPassword = member.toBuilder()
                .isPasswordAuth(true)
                .build();
        memberRepository.save(authResetPassword);

        redisService.deleteAuthCode(email);
    }

    public void resetPassword(String email, String newPassword, Platform platform) {

        Member member = memberRepository.findByEmailAndPlatform(email, platform)
                .orElseThrow(NotFoundMemberException::new);

        if (!member.getIsPasswordAuth()) {

            throw new NotAuthorizedPasswordAuth();
        }

        String encPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        Member passwordReset = member.toBuilder()
                .password(encPassword)
                .isPasswordAuth(false)
                .build();
        memberRepository.save(passwordReset);
    }
}



