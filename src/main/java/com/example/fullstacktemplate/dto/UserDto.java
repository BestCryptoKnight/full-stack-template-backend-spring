package com.example.fullstacktemplate.dto;

import com.example.fullstacktemplate.model.FileDb;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


@Data
public class UserDto {

    @Size(min = 4, message = "name.lengthRestriction")

    private  String name;
    @Email(message = "email.invalidFormat")
    private String email;

    @NotNull(message = "profileImage.null")
    private FileDb profileImage;

    private Boolean twoFactorEnabled;

}
