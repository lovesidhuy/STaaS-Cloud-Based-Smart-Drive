package com.onlinedrive.staas.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("folders")
public class Folder {

    @Id
    private String id;

    @NotBlank
    private String name;
    private long creationTimestamp = System.currentTimeMillis();

    @DBRef
    private User owner;

    @DBRef
    private List<FileItem> files = new ArrayList<>();

    public Folder(String name, User owner) {
        this.name = name.toLowerCase();
        this.owner = owner;
    }

    public void addFile(FileItem file) {
        boolean exists = files.stream().anyMatch(f -> f.getName().equalsIgnoreCase(file.getName()));
        if (exists) {
            throw new IllegalArgumentException("File with name '" + file.getName() + "' already exists in folder");
        }
        files.add(file);
    }

    public void removeFile(String fileName) {
        files.removeIf(f -> f.getName().equals(fileName.toLowerCase()));
    }

    public long getSize() {
        return files.stream().mapToLong(FileItem::getSize).sum();
    }
}
