package com.tenten.studybadge.account.controller;

import com.tenten.studybadge.account.service.AccountService;
import com.tenten.studybadge.common.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "예금주 조회", description = "예금주 조회(계좌인증)")
    @Parameter(name = "BankCode", description = "은행코드")
    @Parameter(name = "BankNum", description = "계좌번호")
    @GetMapping("/api/cert/account")
    public ResponseEntity<Void> certAccount(@LoginUser Long memberId,
                                            @RequestParam String BankCode,
                                            @RequestParam String BankNum) {

        accountService.certAccount(memberId, BankCode, BankNum);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
