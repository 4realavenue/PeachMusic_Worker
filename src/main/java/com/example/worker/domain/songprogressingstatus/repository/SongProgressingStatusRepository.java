package com.example.worker.domain.songprogressingstatus.repository;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SongProgressingStatusRepository extends JpaRepository<SongProgressingStatus, Long> {

    Optional<SongProgressingStatus> findBySong_SongId(Long songId);

    @Query("""
            SELECT sps.song.songId FROM SongProgressingStatus sps
            WHERE sps.progressingStatus = :progressingStatus
            ORDER BY sps.song.songId ASC
            """)
    List<Long> findSongIdListByProgressingStatus(ProgressingStatus progressingStatus, Pageable pageable);

    @Query("""
            SELECT sps FROM SongProgressingStatus sps
            WHERE sps.progressingStatus = :progressingStatus
            """)
    List<SongProgressingStatus> findSongProgressingStatusByProgressingStatus(ProgressingStatus progressingStatus, Pageable pageable);

    Optional<SongProgressingStatus> findSongProgressingStatusBySong_SongId(Long songId);

    @Modifying
    @Transactional
    @Query("""
            UPDATE SongProgressingStatus sps
            SET sps.progressingStatus = :to
            WHERE sps.song.songId = :songId
            AND sps.progressingStatus = :from
            """)
    int claimStatusBySongId(Long songId, ProgressingStatus from, ProgressingStatus to);

    @Modifying
    @Transactional
    @Query("""
            UPDATE SongProgressingStatus sps
            SET sps.progressingStatus = :progressingStatus
            WHERE sps.song.songId = :songId
            """)
    void updateStatusBySongId(Long songId, ProgressingStatus progressingStatus);

    @Modifying
    @Transactional
    @Query("""
            UPDATE SongProgressingStatus sps
            SET sps.progressingStatus = :to
            WHERE sps.song.songId = :songId
            AND sps.progressingStatus in (:fromList)
            """)
    int readyToAbleWork(Long songId, List<ProgressingStatus> fromList, ProgressingStatus to);

    Long song(Song song);
}
