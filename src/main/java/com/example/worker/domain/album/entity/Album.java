package com.example.worker.domain.album.entity;

import com.example.worker.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table (name = "albums")
public class Album extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "album_id")
    private Long albumId;

    @Column(name = "jamendo_album_id", unique = true)
    private Long jamendoAlbumId;

    @Column(name = "album_name", nullable = false)
    private String albumName;

    @Column(name = "album_release_date", nullable = false)
    private LocalDate albumReleaseDate;

    @Column(name = "album_image", nullable = false, unique = true)
    private String albumImage;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public Album(String albumName, LocalDate albumReleaseDate, String albumImage) {
        this.albumName = albumName;
        this.albumReleaseDate = albumReleaseDate;
        this.albumImage = albumImage;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void restore() {
        this.isDeleted = false;
    }

    public void updateAlbumImage(String albumImage) {
        this.albumImage = albumImage;
    }
}