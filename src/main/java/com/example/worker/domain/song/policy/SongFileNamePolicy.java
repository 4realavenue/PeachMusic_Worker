package com.example.worker.domain.song.policy;

import com.example.worker.domain.song.entity.Song;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SongFileNamePolicy {

    // 음원 파일 명명 규칙
    public static String mp3FileNamePolicy(Song song) {

        LocalDateTime createdAt = song.getCreatedAt() != null ? song.getCreatedAt() : LocalDateTime.now();

        String date = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String title = sanitize(song.getName());

        return "peachmusic_song_" + title + "_" + date + ".mp3";
    }

    // 명명 규칙을 위한 공백/특수문자 제거
    public static String sanitize(String input) {

        if (input == null) {
            return "알수없음";
        }

        return input.replaceAll("[\\\\/:*?\"<>|_-]", "").replaceAll("\\s+", "").trim();
    }
}
