package com.example.fullstacktemplate.controller;


import com.example.fullstacktemplate.dto.ApiResponse;
import com.example.fullstacktemplate.exception.BadRequestException;
import com.example.fullstacktemplate.model.FileDb;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class FileController extends Controller {


    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        String message = "";
        try {
            storageService.store(file);

            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse(true, message));
        } catch (Exception e) {
            message = "Could not upload the file: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, message));
        }
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FileDb>> getListFiles() {
        return ResponseEntity.status(HttpStatus.OK).body(storageService.getAllFiles().collect(Collectors.toList()));
    }

    @GetMapping(value = "/file/{id}")
    public ResponseEntity<?> getFile(@PathVariable String id) {
        Optional<FileDb> fileDbOptional = storageService.getFile(id);
        return fileDbOptional
                .<ResponseEntity<?>>map(fileDb -> ResponseEntity.ok().body(fileDb))
                .orElseThrow(() -> new BadRequestException("fileNotExist"));
    }
}
