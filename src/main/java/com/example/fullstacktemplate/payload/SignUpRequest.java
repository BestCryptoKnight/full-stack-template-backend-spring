package com.example.fullstacktemplate.payload;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SignUpRequest {
    @Size(min = 4, message = "name.lengthRestriction")
    private String name;

    @Email(message = "email.invalidFormat")
    private String email;

    @Pattern(message = "password.invalidFormat", regexp = "^(?=.*[\\d])(?=.*[A-Z])(?=.*[a-z])[\\w!@#$%^&*]{8,}$")
    private String password;

}
