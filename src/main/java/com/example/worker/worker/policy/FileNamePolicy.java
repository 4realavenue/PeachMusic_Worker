package com.example.worker.worker.policy;

import com.example.worker.domain.song.dto.SongDto;

import java.time.format.DateTimeFormatter;

public class FileNamePolicy {

    public static String sanitize(String input) {

        if(input == null) return "알수없음";
        return input.replaceAll("[\\\\/:*?\"<>|_-]", "").replaceAll("\\s+", "").trim();
    }

    public static String mp3FileNamePolicy(SongDto metaData) {
        String date = metaData.createdAt().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String title = sanitize(metaData.title());

        return "peachmusic_song_" + title + "_" + date + ".mp3";
    }
}
