package com.onlinedrive.staas.dto;

import java.util.List;

public record UserDTO(
        String id,
        String username,
        long storageQuotaBytes,
        long usedStorageBytes,
        List<FolderDTO> folders
) {
    public long availableBytes() {
        return storageQuotaBytes - usedStorageBytes;
    }
}
