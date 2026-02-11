package com.example.worker.worker.service;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.common.storage.R2StorageService;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.song.policy.SongFileNamePolicy;
import com.example.worker.domain.song.repository.SongRepository;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.worker.AudioDownloader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioDownloadService {

    private final SongRepository songRepository;
    private final SongProgressingStatusRepository songProgressingStatusRepository;
    private final R2StorageService r2StorageService;

    private final static Logger downloadLog = LoggerFactory.getLogger("WORKER_DOWNLOAD");

    // 음원 다운로드 시도 (관리자 수동)
    @Transactional
    public void tryDownloadSong(WorkerTryWorkRequestDto requestDto) {

        List<ProgressingStatus> allowed = List.of(ProgressingStatus.NOT_READY, ProgressingStatus.DOWNLOAD_FAILED);

        for (Long songId : requestDto.getSongIdList()) {

            int claimedDownload = songProgressingStatusRepository.readyToAbleWork(songId, allowed, ProgressingStatus.DOWNLOADING);

            if (claimedDownload == 0) {
                continue;
            }

            try {
                downloadSong(songId);

            } catch (Exception exception) {
                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.DOWNLOAD_FAILED);

                downloadLog.error("SongId : {}, Download Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 음원 다운로드 수행
    @Transactional
    public void downloadSong(Long songId) {

        Song findSong = songRepository.findSongBySongId(songId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 음원입니다"));

        String fileName = SongFileNamePolicy.mp3FileNamePolicy(findSong);

        String tmpDir = System.getProperty("java.io.tmpdir");
        Path localPath = Path.of(tmpDir, "peachmusic", "audio", fileName);

        AudioDownloader.downloadAudio(findSong.getAudio(), localPath);

        String key = "storage/audio/" + fileName;

        r2StorageService.upload(localPath, key, "audio/mpeg");

        findSong.updateAudio(key);

        songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.READY);

        try {
            Files.deleteIfExists(localPath);
        } catch (IOException ignored) {}
    }
}
