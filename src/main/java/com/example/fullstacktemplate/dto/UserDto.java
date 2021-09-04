package com.example.fullstacktemplate.dto;

import com.example.fullstacktemplate.dto.validation.File;
import com.example.fullstacktemplate.model.AuthProvider;
import com.example.fullstacktemplate.model.FileType;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;


@Data
public class UserDto {

    @Size(min = 4, message = "name.lengthRestriction")
    private  String name;
    @Email(message = "email.invalidFormat")
    private String email;

    @File(maxSizeBytes = 10000000, fileTypes = {FileType.IMAGE_JPEG, FileType.IMAGE_PNG}, message = "profileImage.invalidMessage")
    private FileDbDto profileImage;

    private Boolean twoFactorEnabled;

    private Boolean isO2AuthAccount;

}
