package com.onlinedrive.staas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FolderCreateRequest(
        @NotBlank(message = "Folder name is required")
        @Size(max = 255, message = "Folder name must not exceed 255 characters")
        String folderName
) { }
