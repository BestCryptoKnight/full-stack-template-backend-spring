package com.example.fullstacktemplate.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileResponse {

   private UserDto user;
   private String message;

}
