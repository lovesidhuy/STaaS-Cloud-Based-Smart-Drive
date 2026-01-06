package com.onlinedrive.staas.repository;

import com.onlinedrive.staas.model.Folder;
import com.onlinedrive.staas.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FolderRepository extends MongoRepository<Folder, String> {

    Optional<Folder> findByOwnerAndName(User owner, String name);

    Page<Folder> findByOwner(User owner, Pageable pageable);
}
