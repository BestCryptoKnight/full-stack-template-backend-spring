package com.example.fullstacktemplate.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerificationRequest {
    private String code;

}
