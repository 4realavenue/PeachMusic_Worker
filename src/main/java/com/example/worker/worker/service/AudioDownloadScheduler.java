package com.example.worker.worker.service;

import com.example.worker.worker.policy.FileNamePolicy;
import com.example.worker.domain.song.dto.SongDto;
import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioDownloadScheduler {

    private final SongDao songDao;
    private final StreamingJobDao streamingJobDao;

    // 매일 4시에 download OpenApi url 다운로드 진행
    @Scheduled(cron = "0 0 4 * * ?")
//    @Scheduled(fixedDelay = 50000)
    public void downloadAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.NOT_READY, 200);

        for (Long songId : songIdList) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.NOT_READY, JobStatus.DOWNLOADING)) continue;

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
                log.error("SongId : {}, Download Failed : {}",songId, exception.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 6 * * ?")
    public void reDownloadAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.DOWNLOAD_FAILED, 200);

        for (Long songId : songIdList) {
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
