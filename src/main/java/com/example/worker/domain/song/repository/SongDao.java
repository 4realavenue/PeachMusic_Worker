package com.example.worker.domain.song.repository;

import com.example.worker.domain.song.dto.SongDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SongDao {

    private final JdbcTemplate jdbcTemplate;

    public SongDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public SongDto loadMetaData(Long songId) {
        return jdbcTemplate.queryForObject("""
                    SELECT song_id, name, audio, created_at
                    FROM songs
                    WHERE song_id = ?
                """, (rs, i) -> new SongDto(
                rs.getLong("song_id"),
                rs.getString("name"),
                rs.getString("audio"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), songId);
    }

    public void updateStatus(Long songId, boolean status) {
        jdbcTemplate.update("""
                    UPDATE songs
                    SET streaming_status = ?
                    WHERE song_id = ?
                """, status, songId);
    }

    public void updateAudioPath(Long songId, String audioPath) {
        jdbcTemplate.update("""
                    UPDATE songs
                    SET audio = ?
                    WHERE song_id = ?
                """, audioPath, songId);
    }

}

