package com.tenten.studybadge.common.exception.account;

import com.tenten.studybadge.common.exception.basic.AbstractException;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class BeforeCertAccountException extends AbstractException {

    private static final String ERROR_CODE = "BEFORE_CERT_ACCOUNT_EXCEPTION";
    private static final String ERROR_MESSAGE = "계좌 인증이 완료되지 않았습니다.";

    @Override
    public HttpStatus getHttpStatus() {
        return BAD_REQUEST;
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }

    @Override
    public String getMessage() {
        return ERROR_MESSAGE;
    }
}