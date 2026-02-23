package com.example.worker.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;
    private final RestClient.Builder builder;

    @Value("${r2.bucket.media}")
    private String mediaBucket;

    @Value("${r2.bucket.assets}")
    private String assetsBucket;

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

    public void privateUpload(Path file, String key, String contentType) {
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
            throw new RuntimeException("R2 private-upload 실패", e);
        }
    }

    public void publicUpload(Path file, String key, String contentType) {
        try {
            String cacheControl = resolveCacheControl(key);

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                            .bucket(assetsBucket)
                                    .key(key)
                                            .contentType(contentType);

            if (cacheControl != null) {
                requestBuilder.cacheControl(cacheControl);
            }

            s3Client.putObject(requestBuilder.build(), file);

        } catch (Exception e) {
            throw new RuntimeException("R2 public-upload 실패", e);
        }
    }

    public String resolveCacheControl(String key) {
        String lowerCase = key.toLowerCase();

        if (lowerCase.endsWith(".ts")) {
            return "public, max-age=31536000, immutable";
        }

        if (lowerCase.endsWith("m3u8")) {
            return "public, max-age=86400";
        }

        if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".png")) {
            return "public, max-age=86400";
        }

        return null;
    }
}