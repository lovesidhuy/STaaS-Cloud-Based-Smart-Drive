package com.onlinedrive.staas.dto;

public record FileItemDTO(
        String id,
        String name,
        long actualSize,
        String fileExtension,
        String mimeType
) {
}
