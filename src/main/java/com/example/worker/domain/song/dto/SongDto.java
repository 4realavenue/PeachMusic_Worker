package com.example.worker.domain.song.dto;

import java.time.LocalDateTime;

public record SongDto(
        Long songId, String title, String audio, LocalDateTime createdAt


) {}
