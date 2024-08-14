package com.tenten.studybadge.common.exception.account;

import com.tenten.studybadge.common.exception.basic.AbstractException;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class NotMatchAccountHolder extends AbstractException {

    private static final String ERROR_CODE = "NOT_MATCH_ACCOUNT_HOLDER";
    private static final String ERROR_MESSAGE = "예금주와 회원의 이름이 일치하지 않습니다.";

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