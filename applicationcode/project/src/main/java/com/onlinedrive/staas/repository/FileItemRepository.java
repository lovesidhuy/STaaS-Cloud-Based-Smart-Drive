package com.onlinedrive.staas.repository;

import com.onlinedrive.staas.model.FileItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileItemRepository extends MongoRepository<FileItem, String> { }
