package com.onlinedrive.staas.dto;

public record UploadUrlResponse(
        String uploadUrl,
        String fileName,
        String folderName
) {
}
