package com.example.fullstacktemplate.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorResponse {

    private byte[] qrData;
    private String mimeType;
}
