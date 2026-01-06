package com.onlinedrive.staas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UploadUrlRequest(
        @NotBlank(message = "Folder name is required")
        String folderName,

        @NotBlank(message = "File name is required")
        String fileName,

        @Positive(message = "File size must be positive")
        long fileSize,

        @NotBlank(message = "Extension is required")
        String extension
) {
}
