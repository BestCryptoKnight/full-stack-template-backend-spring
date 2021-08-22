package com.example.fullstacktemplate.payload;

import com.example.fullstacktemplate.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileResponse {

   private User user;
   private String message;

}
