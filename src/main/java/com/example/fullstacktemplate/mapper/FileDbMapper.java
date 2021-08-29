package com.example.fullstacktemplate.mapper;

import com.example.fullstacktemplate.dto.CustomMapper;
import com.example.fullstacktemplate.dto.FileDbDto;
import com.example.fullstacktemplate.model.FileDb;
import com.example.fullstacktemplate.model.FileType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = FileDbMapperResolver.class)
public interface FileDbMapper extends CustomMapper<FileDbDto, FileDb> {

    default String fileTypeToString(FileType fileType) {
        return fileType.getMimeType();
    }

    default FileType stringToFileType(String mimeType) {
        return FileType.fromMimeType(mimeType).orElse(null);
    }
}
