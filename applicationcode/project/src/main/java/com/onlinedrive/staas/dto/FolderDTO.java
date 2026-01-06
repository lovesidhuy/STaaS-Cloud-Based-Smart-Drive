package com.onlinedrive.staas.dto;

import java.util.List;

public record FolderDTO(
        String id,
        String name,
        long creationTimestamp,
        List<FileItemDTO> files
) {
}
