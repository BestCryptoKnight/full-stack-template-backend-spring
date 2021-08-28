package com.example.fullstacktemplate.dto;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "password.blank")
    private String currentPassword;

    private String newPassword;
}
