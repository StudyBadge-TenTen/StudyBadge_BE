package com.tenten.studybadge.account.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.IamportClient;
import com.tenten.studybadge.account.dto.AccountResponse;
import com.tenten.studybadge.common.exception.account.NotMatchAccountHolder;
import com.tenten.studybadge.common.exception.member.NotFoundMemberException;
import com.tenten.studybadge.member.domain.entity.Member;
import com.tenten.studybadge.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static com.tenten.studybadge.common.constant.AccountConstant.*;


@Service
@RequiredArgsConstructor
public class AccountService {

    @Value("${portone.api.key}")
    private String impKey;

    @Value("${portone.api.secret}")
    private String impSecret;
    private IamportClient iamportClient;

    private final MemberRepository memberRepository;

    public AccountResponse getAccountHolder(String bankCode, String bankNum) {

        iamportClient = new IamportClient(impKey, impSecret);

        WebClient webClient = WebClient.builder()
                .baseUrl(IamportClient.API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, issueToken())
                .build();

        String response =  webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(PATH)
                        .queryParam(BANK_CODE,bankCode)
                        .queryParam(BANK_NUM, bankNum)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String decode = decodeUnicode(response);

        return AccountResponse.getHolder(decode);
    }

    public void certAccount(Long memberId, String bankCode, String bankNum) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(NotFoundMemberException::new);

        AccountResponse accountResponse = getAccountHolder(bankCode, bankNum);

        if (accountResponse.getAccountHolder().equals(member.getName())) {

            Member updatedMember = member.toBuilder()
                    .isAccountCert(true)
                    .build();

            memberRepository.save(updatedMember);

        } else {

            throw new NotMatchAccountHolder();
        }
    }

    private String decodeUnicode(String response) {
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);

            Map<String, Object> responseDetails = (Map<String, Object>) responseMap.get(RESPONSE);
            String bankHolder = (String) responseDetails.get(BANK_HOLDER);

            return bankHolder;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String issueToken() {

        return iamportClient.getAuth().getResponse().getToken();
    }
}