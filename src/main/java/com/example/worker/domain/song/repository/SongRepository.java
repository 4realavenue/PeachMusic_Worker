package com.example.worker.domain.song.repository;

import com.example.worker.domain.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findSongBySongId(Long songId);
}
