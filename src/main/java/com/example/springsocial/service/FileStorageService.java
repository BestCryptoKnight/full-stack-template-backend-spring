package com.example.springsocial.service;

import com.example.springsocial.model.FileDb;
import com.example.springsocial.repository.FileRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    @Autowired
    private FileRepository fileDBRepository;

    public FileDb store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        FileDb FileDB = new FileDb(fileName, file.getContentType(), file.getBytes());

        return fileDBRepository.save(FileDB);
    }

    public FileDb store(File file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getName());
        FileDb FileDB = new FileDb(fileName, Files.probeContentType(file.toPath()), Files.readAllBytes(file.toPath()));
        return fileDBRepository.save(FileDB);
    }

    public FileDb store(String name, URL url) throws IOException {
        FileDb FileDB = new FileDb(name, url.openConnection().getContentType(), IOUtils.toByteArray(url));
        return fileDBRepository.save(FileDB);
    }

    public Optional<FileDb> getFile(String id) {
        return fileDBRepository.findById(id);
    }

    public Stream<FileDb> getAllFiles() {
        return fileDBRepository.findAll().stream();
    }
}
