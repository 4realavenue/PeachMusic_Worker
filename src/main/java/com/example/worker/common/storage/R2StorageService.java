package com.example.worker.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;

    @Value("${r2.bucket.media}")
    private String mediaBucket;

    public void download(String key, Path localPath) {
        try {
            Files.createDirectories(localPath.getParent());

            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(mediaBucket)
                            .key(key)
                            .build(),
                    localPath
            );
        } catch (Exception e) {
            throw new RuntimeException("R2 download 실패", e);
        }
    }

    public void upload(Path file, String key, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(mediaBucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    file
            );
        } catch (Exception e) {
            throw new RuntimeException("R2 upload 실패", e);
        }
    }
}