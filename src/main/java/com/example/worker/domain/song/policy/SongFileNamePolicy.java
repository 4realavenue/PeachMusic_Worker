package com.example.worker.domain.song.policy;

import com.example.worker.domain.song.entity.Song;

public class SongFileNamePolicy {

    // 음원 파일 명명 규칙
    public static String mp3FileNamePolicy(Song song) {
        return song.getSongId().toString() + ".mp3";
    }
}
