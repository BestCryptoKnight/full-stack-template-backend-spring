package com.example.fullstacktemplate.dto.mapper;

import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.FileDb;
import com.example.fullstacktemplate.service.FileDbService;
import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

@Component
public class FileDbMapperResolver {


    private final FileDbService fileDbService;

    public FileDbMapperResolver(FileDbService fileDbService) {
        this.fileDbService = fileDbService;
    }

    @ObjectFactory
    public FileDb resolve(Long id){
        return fileDbService.findById(id).orElseThrow(()->new BadRequestException("fileNotExist"));
    }
}
