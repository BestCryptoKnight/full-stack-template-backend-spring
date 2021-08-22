package com.example.fullstacktemplate.payload;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class TokenAccessRequest {

    @NotBlank(message = "token.blank")
    private String token;

}
