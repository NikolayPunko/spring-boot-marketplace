package com.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BackupService {

    @Value("${backup.pgDumpPath}")
    private String pgDumpPath;

    @Value("${backup.psqlPath}")
    private String psqlPath;

    @Value("${backup.host}")
    private String host;

    @Value("${backup.port}")
    private String port;

    @Value("${backup.dbName}")
    private String dbName;

    @Value("${backup.user}")
    private String user;

    @Value("${backup.password}")
    private String password;

    @Value("${backup.dir}")
    private String backupDir;

    public Path createBackupFile() {

        try {
            Files.createDirectories(Paths.get(backupDir));

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "backup_" + dbName + "_" + ts + ".sql";
            Path outFile = Paths.get(backupDir).resolve(fileName);

            List<String> cmd = List.of(
                    pgDumpPath,
                    "-h", host,
                    "-p", port,
                    "-U", user,
                    "-d", dbName,
                    "--clean",
                    "--if-exists",
                    "--no-owner",
                    "--no-privileges",
                    "-f", outFile.toAbsolutePath().toString()
            );

            runProcess(cmd);

            if (!Files.exists(outFile) || Files.size(outFile) == 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup file is empty");
            }

            return outFile;

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup failed: " + e.getMessage());
        }
    }

    public byte[] readBackup(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Read backup failed: " + e.getMessage());
        }
    }

    public void restoreFromUploadedFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }

        try {
            Files.createDirectories(Paths.get(backupDir));

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path tempSql = Paths.get(backupDir).resolve("restore_" + ts + ".sql");

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, tempSql, StandardCopyOption.REPLACE_EXISTING);
            }

            // psql -f file.sql
            List<String> cmd = List.of(
                    psqlPath,
                    "-h", host,
                    "-p", port,
                    "-U", user,
                    "-d", dbName,
                    "-f", tempSql.toAbsolutePath().toString()
            );

            runProcess(cmd);

            // можно удалить временный файл
            Files.deleteIfExists(tempSql);

        } catch (IOException e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Restore failed: " + e.getMessage());
        }
    }

    private void runProcess(List<String> cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);

            // пароль через env (psql/pg_dump читают PGPASSWORD)
            Map<String, String> env = pb.environment();
            env.put("PGPASSWORD", password);

            pb.redirectErrorStream(true);

            Process p = pb.start();

            String out;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                out = sb.toString();
            }

            int code = p.waitFor();
            if (code != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Command failed (" + code + "):\n" + out);
            }

        } catch (ResponseStatusException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Process error: " + e.getMessage());
        }
    }
}