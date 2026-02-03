package com.example.worker.worker.service;

import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.dto.SongDto;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.dto.RetryRequestDto;
import com.example.worker.worker.policy.FileNamePolicy;
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

    public void retryDownloadSong(RetryRequestDto requestDto) {

        for (Long songId : requestDto.getSongIdList()) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.DOWNLOAD_FAILED, JobStatus.DOWNLOADING)) continue;

            try {
                SongDto songMetaData = songDao.loadMetaData(songId);

                String fileName = FileNamePolicy.mp3FileNamePolicy(songMetaData);

                Path savePath = Path.of("uploads/audios/" + fileName);

                String path = savePath.toString().replace("\\", "/");

                songDao.updateAudioPath(songId, path);

                streamingJobDao.updateStatus(songId, JobStatus.READY);

                AudioDownloader.downloadAudio(songMetaData.audio(), savePath);
            } catch (Exception exception) {
                streamingJobDao.updateStatus(songId, JobStatus.DOWNLOAD_FAILED);
                log.error("SongId : {}, Download Failed : {}", songId, exception.getMessage());
            }

        }
    }
}
