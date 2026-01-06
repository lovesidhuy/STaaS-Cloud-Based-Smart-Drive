package com.onlinedrive.staas.service;

import com.onlinedrive.staas.dto.*;
import com.onlinedrive.staas.GlobalExceptionHandler.NotFoundException;
import com.onlinedrive.staas.mapper.*;
import com.onlinedrive.staas.model.*;
import com.onlinedrive.staas.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnlineDriveService {

    private final UserRepository users;
    private final FolderRepository folders;
    private final FileItemRepository files;
    private final S3Service s3Service;

    private final UserMapper userMapper;
    private final FolderMapper folderMapper;
    private final FileItemMapper fileItemMapper;

    private User requireUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
    }

    private Folder requireFolder(User user, String folderName) {
        return folders.findByOwnerAndName(user, folderName.toLowerCase())
                .orElseThrow(() -> new NotFoundException("Folder not found: " + folderName));
    }

    private FileItem requireFile(Folder folder, String fileName) {
        return folder.getFiles().stream()
                .filter(f -> f.getName().equals(fileName.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("File not found: " + fileName));
    }

    private void performDeleteOperation(User user, java.util.List<FileItem> filesToDelete) {
        filesToDelete.forEach(file -> {
            files.delete(file);
            user.freeSpace(file.getSize());
        });
        users.save(user);
    }

    public void registerUser(User user) {
        if (users.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        users.save(user);
        log.info("User registered: {}", user.getUsername());
    }

    public User login(String username, String password) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        if (!user.verifyPassword(password)) {
            throw new IllegalArgumentException("Invalid password");
        }
        log.info("Login success: {}", username);
        return user;
    }

    public Page<UserDTO> listUsers(Pageable pageable) {
        return users.findAll(pageable).map(userMapper::toDto);
    }

    public UserDTO getUserDTO(String username) {
        User user = requireUser(username);
        return userMapper.toDto(user);
    }

    public FolderDTO createFolder(String username, String folderName) {
        User user = requireUser(username);
        Folder folder = new Folder(folderName, user);
        folders.save(folder);
        user.addFolder(folder);
        users.save(user);
        log.info("Folder created: {} in {}", folderName, username);
        return folderMapper.toDto(folder);
    }

    public Page<FolderDTO> getUserFolders(String username, Pageable pageable) {
        User user = requireUser(username);
        return folders.findByOwner(user, pageable).map(folderMapper::toDto);
    }

    public void deleteFolder(String username, String folderName) {
        User user = requireUser(username);
        Folder folder = requireFolder(user, folderName);

        folder.getFiles().forEach(file -> {
            if (file.getS3ObjectKey() != null && !file.getS3ObjectKey().isEmpty()) {
                s3Service.deleteObject(file.getS3ObjectKey());
            }
        });

        performDeleteOperation(user, folder.getFiles());
        folders.delete(folder);
        log.info("Folder deleted: {} from {}", folderName, username);
    }

    public FileItemDTO updateFile(String username, String folderName, String oldName, String newName) {
        User user = requireUser(username);
        Folder folder = requireFolder(user, folderName);
        FileItem file = requireFile(folder, oldName);

        boolean conflict = folder.getFiles().stream()
                .anyMatch(f -> !f.equals(file) && f.getName().equals(newName.toLowerCase()));
        if (conflict) {
            throw new IllegalArgumentException("File with name '" + newName.toLowerCase() + "' already exists in folder");
        }

        file.rename(newName);
        files.save(file);
        log.info("File renamed: {} to {} in {}", oldName, newName, folderName);
        return fileItemMapper.toDto(file);
    }

    public void deleteFile(String username, String folderName, String fileName) {
        User user = requireUser(username);
        Folder folder = requireFolder(user, folderName);
        FileItem file = requireFile(folder, fileName);

        if (file.getS3ObjectKey() != null && !file.getS3ObjectKey().isEmpty()) {
            s3Service.deleteObject(file.getS3ObjectKey());
        }

        folder.removeFile(fileName);
        folders.save(folder);
        performDeleteOperation(user, java.util.List.of(file));
        log.info("File deleted: {} from {}", fileName, folderName);
    }

    private String getContentType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        return switch (extension.toLowerCase()) {
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            case "zip" -> "application/zip";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    public java.util.Map<String, String> generateUploadUrl(String username, String folderName, String fileName, long size, String ext) {
        User user = requireUser(username);
        Folder folder = requireFolder(user, folderName);

        if (user.getUsedStorageBytes() + size > user.getStorageQuotaBytes()) {
            throw new IllegalArgumentException("Upload exceeds storage quota of " + user.getStorageQuotaBytes() + " bytes");
        }

        String normalizedFileName = fileName.toLowerCase();

        String objectKey = s3Service.generateObjectKey(username, folderName, normalizedFileName);

        FileItem file = new FileItem(normalizedFileName, user, size, ext);
        file.setS3ObjectKey(objectKey);
        file.setParentFolder(folder);

        files.save(file);
        folder.addFile(file);
        folders.save(folder);

        user.consumeSpace(size);
        users.save(user);

        String contentType = getContentType(ext);
        String uploadUrl = s3Service.generatePresignedUploadUrl(objectKey, contentType).toString();

        log.info("Upload URL generated for: {} ({} bytes, {}) in {}", normalizedFileName, size, contentType, folderName);

        return java.util.Map.of(
                "uploadUrl", uploadUrl,
                "objectKey", objectKey
        );
    }

    public String generateDownloadUrl(String username, String folderName, String fileName) {
        User user = requireUser(username);
        Folder folder = requireFolder(user, folderName);
        FileItem file = requireFile(folder, fileName);

        if (file.getS3ObjectKey() == null || file.getS3ObjectKey().isEmpty()) {
            throw new IllegalArgumentException("File does not have an S3 object key");
        }

        String downloadUrl = s3Service.generatePresignedDownloadUrl(file.getS3ObjectKey(), file.getName()).toString();
        log.info("Download URL generated for: {} from {}", fileName, folderName);

        return downloadUrl;
    }
}