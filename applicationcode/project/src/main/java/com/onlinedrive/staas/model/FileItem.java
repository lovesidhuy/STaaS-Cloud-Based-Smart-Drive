package com.onlinedrive.staas.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("files")
public class FileItem {

    @Id
    private String id;

    @NotBlank
    private String name;

    @Positive
    private long actualSize;

    @NotBlank
    private String fileExtension;
    private String mimeType;
    private String s3ObjectKey;
    private long creationTimestamp = System.currentTimeMillis();

    @DBRef private Folder parentFolder;
    @DBRef private User owner;

    public FileItem(String name, User owner, long actualSize, String ext) {
        this.name = name.toLowerCase();
        this.owner = owner;
        this.actualSize = actualSize;
        this.fileExtension = ext.toLowerCase();
        this.mimeType = guessMimeType(ext);
    }

    public long getSize() { return actualSize; }

    public void rename(String newName) {
        this.name = newName.toLowerCase();
    }

    private String guessMimeType(String ext) {
        return switch (ext.toLowerCase()) {
            case "txt" -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
