package com.tenten.studybadge.account.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountResponse {

    private String accountHolder;

    public static AccountResponse getHolder(String response) {

        return AccountResponse.builder()
                .accountHolder(response)
                .build();
    }
}
