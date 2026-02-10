package com.example.worker.domain.songprogressingstatus.entity;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.song.entity.Song;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "song_progressing_status")
public class SongProgressingStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "song_progressing_status_id")
    private Long songProgressingStatusId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "song_id", nullable = false)
    private Song song;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "progressing_status", nullable = false)
    private ProgressingStatus progressingStatus;

    public SongProgressingStatus(Song song, ProgressingStatus progressingStatus) {
        this.song = song;
        this.progressingStatus = progressingStatus;
    }

    public void updateStatus(ProgressingStatus progressingStatus) {
        this.progressingStatus = progressingStatus;
    }
}