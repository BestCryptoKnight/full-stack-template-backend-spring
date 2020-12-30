package com.example.springsocial.payload;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerificationRequest {
    private String code;

}
