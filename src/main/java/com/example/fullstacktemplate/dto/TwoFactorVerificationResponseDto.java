package com.example.fullstacktemplate.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TwoFactorVerificationResponseDto {
    private List<String> verificationCodes;
}
