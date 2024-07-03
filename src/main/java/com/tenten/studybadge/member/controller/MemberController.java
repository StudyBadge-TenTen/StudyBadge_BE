package com.tenten.studybadge.member.controller;

import com.tenten.studybadge.common.component.AwsS3Service;
import com.tenten.studybadge.member.dto.MemberSignUpRequest;
import com.tenten.studybadge.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import static com.tenten.studybadge.type.member.Platform.LOCAL;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member API", description = "Member API")
public class MemberController {

    private final AwsS3Service awsS3Service;
    private final MemberService memberService;
    @Operation(summary = "회원가입", description = "회원가입")
    @Parameter(name = "signUpRequest", description = "회원가입 요청 Dto" )
    @PostMapping("/sign-up")
    public void signUp(@Valid @RequestPart(value = "signUpRequest") MemberSignUpRequest signUpRequest,
                       @RequestPart(value = "file", required = false) MultipartFile profile) {

        String imgUrl = awsS3Service.uploadFile(profile);
        signUpRequest.setImgUrl(imgUrl);
        memberService.signUp(signUpRequest, LOCAL);
    }
    @Operation(summary = "인증", description = "인증 요청")
    @Parameter(name = "email", description = "이메일")
    @Parameter(name = "code", description = "인증코드")
    @PostMapping("/auth")
    public void auth(@RequestParam(name = "email") String email,
                     @RequestParam(name = "code") String code) {

        memberService.auth(email, code, LOCAL);

    }
}
