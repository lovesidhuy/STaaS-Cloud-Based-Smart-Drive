package com.onlinedrive.staas.controller;

import com.onlinedrive.staas.dto.*;
import com.onlinedrive.staas.model.*;
import com.onlinedrive.staas.service.OnlineDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drive")
@CrossOrigin("*")
@RequiredArgsConstructor
public class DriveController {

    private final OnlineDriveService drive;

    // Pagination
    private Pageable createPageable(int page, int size, Sort sort) {
        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;
        return PageRequest.of(page, size, sort);
    }

    @GetMapping("/")
    public String health() {
        return " STaaS backend running";
    }

    // Users
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody User user) {
        drive.registerUser(user);
        RegisterResponse response = new RegisterResponse(user.getUsername(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        User user = drive.login(request.username(), request.password());
        LoginResponse response = new LoginResponse(user.getUsername(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> listUsers(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size,
                                                                @RequestParam(defaultValue = "username") String sortBy) {
        Page<UserDTO> users = drive.listUsers(createPageable(page, size, Sort.by(sortBy).ascending()));
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    // Folders
    @PostMapping("/{username}/folders")
    public ResponseEntity<ApiResponse<FolderDTO>> createFolder(@PathVariable String username,
                                                               @Valid @RequestBody FolderCreateRequest request) {
        FolderDTO dto = drive.createFolder(username, request.folderName());
        return ResponseEntity.ok(ApiResponse.success("Folder created successfully", dto));
    }

    @GetMapping("/{username}/folders")
    public ResponseEntity<ApiResponse<Page<FolderDTO>>> getUserFolders(@PathVariable String username,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = createPageable(page, size, Sort.by("creationTimestamp").descending());
        Page<FolderDTO> folders = drive.getUserFolders(username, pageable);
        return ResponseEntity.ok(ApiResponse.success("Folders retrieved successfully", folders));
    }

    @DeleteMapping("/{username}/folders/{folderName}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteFolder(@PathVariable String username,
                                                                    @PathVariable String folderName) {
        drive.deleteFolder(username, folderName);
        DeleteResponse response = new DeleteResponse(folderName, "folder");
        return ResponseEntity.ok(ApiResponse.success("Folder deleted successfully", response));
    }

    // Files
    @PutMapping("/{username}/files")
    public ResponseEntity<ApiResponse<FileItemDTO>> updateFile(@PathVariable String username,
                                                               @Valid @RequestBody FileUpdateRequest request) {
        FileItemDTO dto = drive.updateFile(username, request.folderName(), request.oldName(), request.newName());
        return ResponseEntity.ok(ApiResponse.success("File updated successfully", dto));
    }

    @DeleteMapping("/{username}/files")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteFile(@PathVariable String username,
                                                                  @Valid @RequestBody FileDeleteRequest request) {
        drive.deleteFile(username, request.folderName(), request.fileName());
        DeleteResponse response = new DeleteResponse(request.fileName(), "file");
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", response));
    }

    // Drive
    @GetMapping("/{username}/drive")
    public ResponseEntity<ApiResponse<UserDTO>> viewDrive(@PathVariable String username) {
        UserDTO dto = drive.getUserDTO(username);
        return ResponseEntity.ok(ApiResponse.success("Drive retrieved successfully", dto));
    }

    // S3 Presigned URLs

    @PostMapping("/{username}/files/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> generateUploadUrl(
            @PathVariable String username,
            @Valid @RequestBody UploadUrlRequest request) {
        java.util.Map<String, String> result = drive.generateUploadUrl(
                username,
                request.folderName(),
                request.fileName(),
                request.fileSize(),
                request.extension()
        );
        UploadUrlResponse response = new UploadUrlResponse(
                result.get("uploadUrl"),
                request.fileName(),
                request.folderName()
        );
        return ResponseEntity.ok(ApiResponse.success("Upload URL generated successfully", response));
    }

    @GetMapping("/{username}/files/download-url")
    public ResponseEntity<ApiResponse<DownloadUrlResponse>> generateDownloadUrl(
            @PathVariable String username,
            @RequestParam String folderName,
            @RequestParam String fileName) {
        String downloadUrl = drive.generateDownloadUrl(username, folderName, fileName);
        DownloadUrlResponse response = new DownloadUrlResponse(downloadUrl, fileName, folderName);
        return ResponseEntity.ok(ApiResponse.success("Download URL generated successfully", response));
    }
}
