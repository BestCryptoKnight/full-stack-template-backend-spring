package com.example.springsocial.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenResponse {

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    private String accessToken;
}
