package com.tenten.studybadge.common.token.controller;

import com.tenten.studybadge.common.token.service.TokenService;
import com.tenten.studybadge.common.utils.CookieUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tenten.studybadge.common.constant.TokenConstant.AUTHORIZATION;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Tag(name = "Token API", description = "Token API")
public class TokenController {

    private final TokenService tokenService;
    @Operation(summary = "토큰 재발급", description = "토큰 재발급", security = @SecurityRequirement(name = "bearerToken"))
    @PostMapping("/re-issue")
    public ResponseEntity<String> reissue(@RequestHeader(AUTHORIZATION) String token) {

        String response = tokenService.reissue(token);
        ResponseCookie addCookie = CookieUtils.addCookie(response);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, addCookie.toString())
                .body(response);
    }
}
