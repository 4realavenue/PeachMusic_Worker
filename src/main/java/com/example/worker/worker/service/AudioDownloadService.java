package com.example.worker.worker.service;

import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.dto.SongDto;
import com.example.worker.domain.song.policy.SongFileNamePolicy;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.worker.AudioDownloader;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioDownloadService {

    private final SongDao songDao;
    private final StreamingJobDao streamingJobDao;

    // 음원 다운로드 시도 (관리자 수동)
    public void tryDownloadSong(WorkerTryWorkRequestDto requestDto) {

        for (Long songId : requestDto.getSongIdList()) {
            boolean claimed = streamingJobDao.claimStatus(songId, JobStatus.NOT_READY, JobStatus.DOWNLOADING) || streamingJobDao.claimStatus(songId, JobStatus.DOWNLOAD_FAILED, JobStatus.DOWNLOADING);

            if (!claimed) continue;

            try {
                SongDto songMetaData = songDao.loadMetaData(songId);

                String fileName = SongFileNamePolicy.mp3FileNamePolicy(songMetaData);

                Path savePath = Path.of("uploads/audios/" + fileName);

                AudioDownloader.downloadAudio(songMetaData.audio(), savePath);

                String path = savePath.toString().replace("\\", "/");

                songDao.updateAudioPath(songId, path);

                streamingJobDao.updateStatus(songId, JobStatus.READY);

            } catch (Exception exception) {
                streamingJobDao.updateStatus(songId, JobStatus.DOWNLOAD_FAILED);

                log.error("SongId : {}, Download Failed : {}", songId, exception.getMessage());
            }
        }
    }
}
