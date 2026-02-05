package com.example.worker.worker;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class AudioDownloader {

    /**
     * url -> mp3 음원 다운로드 로직
     */
    public static Path downloadAudio(String audioUrl, Path savePath) {
        try (InputStream inputStream = new URL(audioUrl).openStream()) {
            Files.createDirectories(savePath.getParent());

            Files.copy(inputStream, savePath, StandardCopyOption.REPLACE_EXISTING);

            return savePath;

        } catch (Exception exception) {
            throw new RuntimeException("Audio 다운로드에 실패 했습니다. : " + exception.getMessage());
        }
    }
}
