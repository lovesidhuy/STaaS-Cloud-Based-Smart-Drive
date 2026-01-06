package com.onlinedrive.staas.dto;

import jakarta.validation.constraints.NotBlank;

public record FileDeleteRequest(
        @NotBlank(message = "Folder name is required")
        String folderName,
        @NotBlank(message = "File name is required")
        String fileName
) {}
