package com.example.fullstacktemplate.dto.mapper;

import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.User;
import com.example.fullstacktemplate.repository.UserRepository;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component
public class UserMapperResolver {


    private final UserRepository userRepository;

    public UserMapperResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @ObjectFactory
    public User resolve(Long id){
        return userRepository.findById(id).orElseThrow(()->new BadRequestException("userNotFound"));
    }
}
