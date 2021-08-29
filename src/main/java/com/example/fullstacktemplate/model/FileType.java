package com.example.fullstacktemplate.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum FileType {

    IMAGE_JPEG("image/jpeg"),
    IMAGE_PNG("image/png");

    private final String mimeType;

    FileType(String mimeType) {
        this.mimeType = mimeType;
    }

    public static Optional<FileType> fromMimeType(String searchedMimeType){
      return  Arrays.stream(values())
                .filter(fileType->fileType.getMimeType().equals(searchedMimeType))
                .findFirst();
    }

}
