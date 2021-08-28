package com.example.fullstacktemplate.dto;

import com.example.fullstacktemplate.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileResponse {

   private UserDto user;
   private String message;

}
