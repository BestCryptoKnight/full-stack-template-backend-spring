package com.example.fullstacktemplate.repository;

import com.example.fullstacktemplate.model.FileDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileDbRepository extends JpaRepository<FileDb, Long> {


}
