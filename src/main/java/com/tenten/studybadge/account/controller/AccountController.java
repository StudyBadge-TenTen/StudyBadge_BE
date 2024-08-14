package com.tenten.studybadge.account.controller;

import com.tenten.studybadge.account.dto.AccountRequest;
import com.tenten.studybadge.account.service.AccountService;
import com.tenten.studybadge.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "계좌 인증", description = "회원정보 수정 시 계좌인증(예금주조회)")
    @Parameter(name = "BankCode", description = "은행코드")
    @Parameter(name = "BankNum", description = "계좌번호")
    @GetMapping("/api/cert/account")
    public ResponseEntity<Void> certAccount(@LoginUser Long memberId,
                                            @RequestBody AccountRequest accountRequest) {

        accountService.certAccount(memberId, accountRequest);

        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "계좌 인증", description = "회원가입 시 계좌인증(예금주조회)")
    @Parameter(name = "bankCode", description = "은행코드")
    @Parameter(name = "bankNum", description = "계좌번호")
    @Parameter(name = "name", description = "회원가입 시 입력하는 이름")
    @GetMapping("/api/cert/sign-up")
    public ResponseEntity<Void> certSignUp(@RequestParam String bankCode,
                                           @RequestParam String bankNum,
                                           @RequestParam String name) {

        accountService.certSignUp(bankCode, bankNum, name);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
