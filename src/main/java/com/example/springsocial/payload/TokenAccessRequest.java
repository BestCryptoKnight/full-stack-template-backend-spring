package com.example.springsocial.payload;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class TokenAccessRequest {

    @NotBlank(message = "token.blank")
    private String token;

}
