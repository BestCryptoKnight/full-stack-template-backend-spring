package com.example.fullstacktemplate.mapper;


import com.example.fullstacktemplate.dto.CustomMapper;
import com.example.fullstacktemplate.dto.UserDto;
import com.example.fullstacktemplate.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = UserMapperResolver.class)
public interface UserMapper extends CustomMapper<UserDto, User> {

    @Override
    @Mapping(source = "dto.email", target = "requestedNewEmail")
    @Mapping(target = "email",ignore = true)
    User toEntity(Long id, UserDto dto);

}
