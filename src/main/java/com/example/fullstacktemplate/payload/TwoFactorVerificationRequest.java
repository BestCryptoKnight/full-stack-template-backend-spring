package com.example.fullstacktemplate.payload;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerificationRequest {
    private String code;

}
