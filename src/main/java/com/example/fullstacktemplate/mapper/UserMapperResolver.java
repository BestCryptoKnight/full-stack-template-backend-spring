package com.example.fullstacktemplate.mapper;

import com.example.fullstacktemplate.exception.UserNotFoundException;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.service.UserService;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component
public class UserMapperResolver {


    private final UserService userService;

    public UserMapperResolver(UserService userService) {
        this.userService = userService;
    }


    @ObjectFactory
    public User resolve(Long id){
        return userService.findById(id).orElseThrow(UserNotFoundException::new);
    }
}
