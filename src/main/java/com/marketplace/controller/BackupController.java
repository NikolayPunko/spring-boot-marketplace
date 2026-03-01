package com.marketplace.controller;

import com.marketplace.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    // 1) Backup по кнопке: скачать файл
    @PostMapping("/create")
    public ResponseEntity<byte[]> createBackup() {

        Path file = backupService.createBackupFile();
        byte[] bytes = backupService.readBackup(file);

        String filename = file.getFileName().toString();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    // 2) Restore: загрузить .sql
    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> restore(@RequestPart("file") MultipartFile file) {
        backupService.restoreFromUploadedFile(file);
        return Map.of("status", "ok");
    }
}