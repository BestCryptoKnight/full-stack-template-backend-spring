package com.example.fullstacktemplate.service;

import com.example.fullstacktemplate.model.FileDb;
import com.example.fullstacktemplate.model.FileType;
import com.example.fullstacktemplate.repository.FileDbRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Optional;

@Service
public class FileDbService {

    private final FileDbRepository fileDBRepository;

    public FileDbService(FileDbRepository fileDBRepository) {
        this.fileDBRepository = fileDBRepository;
    }

    public FileDb save(String name, FileType type, byte[] data) {
        FileDb FileDB = new FileDb(name, type, data);
        return fileDBRepository.save(FileDB);
    }

    @Cacheable(cacheNames = "file", key = "#id")
    public Optional<FileDb> findById(Long id) {
        return fileDBRepository.findById(id);
    }

}
