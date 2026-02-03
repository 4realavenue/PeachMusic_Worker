package com.example.worker.worker.service;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class AudioDownloader {

    public static Path downloadAudio(String audioUrl, Path savePath) {
        try (InputStream inputStream = new URL(audioUrl).openStream()) {
            Files.createDirectories(savePath.getParent());
            Files.copy(inputStream, savePath, StandardCopyOption.REPLACE_EXISTING);
            return savePath;

        } catch (Exception exception) {
            throw new RuntimeException();
        }
    }
}
