package com.example.fullstacktemplate.dto.mapper;


import com.example.fullstacktemplate.dto.UserDto;
import com.example.fullstacktemplate.model.AuthProvider;
import com.example.fullstacktemplate.model.User;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {UserMapperResolver.class, FileDbMapper.class})
public interface UserMapper extends CustomMapper<UserDto, User> {

    @Override
    @Mapping(source = "dto.email", target = "requestedNewEmail")
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "twoFactorEnabled", ignore = true)
    User toEntity(Long id, UserDto dto);

    @Override
    @Mapping(target = "isO2AuthAccount", source = "authProvider", qualifiedByName = "providerToIsO2AuthAccount")
    UserDto toDto(User user);

    @Named("providerToIsO2AuthAccount")
    default Boolean providerToIsO2AuthAccount(AuthProvider authProvider) {
        return authProvider != AuthProvider.local;
    }

}
