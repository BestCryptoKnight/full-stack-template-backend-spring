package com.example.springsocial.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorResponse {

    private byte[] qrData;
    private String mimeType;
}
