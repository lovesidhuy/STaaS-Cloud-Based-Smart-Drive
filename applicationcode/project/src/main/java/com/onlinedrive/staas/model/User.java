package com.onlinedrive.staas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("users")
public class User {

    @Id
    private String id;

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @Positive(message = "The Storage quota must be greater than 0")
    private long storageQuotaBytes;
    private long usedStorageBytes;

    @DBRef(lazy = true)
    private List<Folder> folders = new ArrayList<>();

    public boolean verifyPassword(String password) {
        return this.password != null && this.password.equals(password);
    }

    public void consumeSpace(long bytes) { usedStorageBytes += bytes; }

    public void freeSpace(long bytes) {
        usedStorageBytes = Math.max(0, usedStorageBytes - bytes);
    }

    public void addFolder(Folder folder) { folders.add(folder); }
}
