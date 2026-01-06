package com.onlinedrive.staas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FileUpdateRequest(
        @NotBlank(message = "Folder name is required")
        @Size(max = 255, message = "Folder name must not exceed 255 characters")
        String folderName,

        @NotBlank(message = "Old name is required")
        @Size(max = 255, message = "Old name must not exceed 255 characters")
        String oldName,

        @NotBlank(message = "New name is required")
        @Size(max = 255, message = "New name must not exceed 255 characters")
        String newName
) { }
