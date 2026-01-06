package com.onlinedrive.staas.dto;

public record DownloadUrlResponse(
        String downloadUrl,
        String fileName,
        String folderName
) {
}
