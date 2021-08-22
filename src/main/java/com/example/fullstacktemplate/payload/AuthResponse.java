package com.example.fullstacktemplate.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private Boolean twoFactorRequired;
    private String tokenType = "Bearer";
    private String message;

}
