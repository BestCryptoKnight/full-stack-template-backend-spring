package com.example.fullstacktemplate.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TwoFactorVerificationResponse {
    private List<String> verificationCodes;
}
