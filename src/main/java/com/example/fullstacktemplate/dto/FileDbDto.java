package com.example.fullstacktemplate.dto;


import com.example.fullstacktemplate.model.FileType;
import lombok.Data;

@Data
public class FileDbDto {

    private String name;
    private String type;
    private byte[] data;
}
