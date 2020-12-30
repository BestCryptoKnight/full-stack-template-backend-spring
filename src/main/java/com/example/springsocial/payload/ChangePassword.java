package com.example.springsocial.payload;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class ChangePassword {

    @NotBlank(message = "password.blank")
    private String currentPassword;

    private String newPassword;
}
