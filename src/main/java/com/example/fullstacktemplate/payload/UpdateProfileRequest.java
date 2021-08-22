package com.example.fullstacktemplate.payload;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateProfileRequest {

    @Size(min = 4, message = "name.lengthRestriction")
    private String name;

    @Email(message = "email.invalidFormat")
    private String email;
    
    private Base64PayloadFile profileImage;

}
