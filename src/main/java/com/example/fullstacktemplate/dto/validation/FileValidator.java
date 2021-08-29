package com.example.fullstacktemplate.dto.validation;


import com.example.fullstacktemplate.dto.FileDbDto;
import com.example.fullstacktemplate.model.FileType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

public class FileValidator implements ConstraintValidator<File, FileDbDto> {

    private List<FileType> fileTypes;
    private int maxSizeBytes;

    @Override
    public void initialize(File fileAnnotation) {
        fileTypes = List.of(fileAnnotation.fileTypes());
        maxSizeBytes = fileAnnotation.maxSizeBytes();
    }

    @Override
    public boolean isValid(FileDbDto fileDbDto, ConstraintValidatorContext constraintValidatorContext) {
        byte[] data = fileDbDto.getData();
        return getMimeType(data)
                .flatMap(FileType::fromMimeType)
                .stream()
                .anyMatch(fileTypes::contains)
                && data.length <= maxSizeBytes;

    }

    private Optional<String> getMimeType(byte[] data) {
        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(data));) {
            return Optional.ofNullable(URLConnection.guessContentTypeFromStream(is));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
