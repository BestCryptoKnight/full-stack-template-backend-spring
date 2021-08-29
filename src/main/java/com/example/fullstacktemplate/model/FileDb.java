package com.example.fullstacktemplate.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "file")
@Getter
@Setter
public class FileDb {

    public FileDb() {

    }

    public FileDb(String name, FileType type, byte[] data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private FileType type;

    @Lob
    @Column(length = 20971520)
    private byte[] data;
}
