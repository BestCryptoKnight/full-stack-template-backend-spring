package com.example.fullstacktemplate.dto.mapper;

public interface CustomMapper<DTO,ENTITY> {

    DTO toDto(ENTITY entity);

    ENTITY toEntity(Long id, DTO dto);
}
