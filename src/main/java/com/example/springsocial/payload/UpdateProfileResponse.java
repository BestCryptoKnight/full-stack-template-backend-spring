package com.example.springsocial.payload;

import com.example.springsocial.model.User;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Getter
@Setter
public class UpdateProfileResponse {

   private User user;
   private String message;

}
