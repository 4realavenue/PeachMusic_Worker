package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.dto.SongDto;
import com.example.worker.domain.song.policy.SongFileNamePolicy;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.AudioDownloader;
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

    // 매일 04시에 url -> mp3 다운로드 진행
    @Scheduled(cron = "0 0 4 * * ?")
//    @Scheduled(fixedDelay = 50000)
    public void downloadAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.NOT_READY, 300);

        for (Long songId : songIdList) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.NOT_READY, JobStatus.DOWNLOADING)) continue;

            try {
                SongDto songMetaData = songDao.loadMetaData(songId);

                String fileName = SongFileNamePolicy.mp3FileNamePolicy(songMetaData);

                // todo 스토리지에 환경에 맞춰 변경 예정
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

    // 매일 06시에 url -> mp3 다운로드 재시도
    @Scheduled(cron = "0 0 6 * * ?")
    public void retryDownloadAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.DOWNLOAD_FAILED, 300);

        for (Long songId : songIdList) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.DOWNLOAD_FAILED, JobStatus.DOWNLOADING)) continue;

            try {
                SongDto songMetaData = songDao.loadMetaData(songId);

                String fileName = SongFileNamePolicy.mp3FileNamePolicy(songMetaData);

                // todo 스토리지에 환경에 맞춰 변경 예정
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

    // 매일 01시에 DOWNLOADING으로 상태가 멈춰 있으면 작업 가능 상태로 복구
    @Scheduled(cron = "0 0 1 * * ?")
//        @Scheduled(fixedDelay = 50000)
    public void updateStatusDownloadingToNotReady() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.DOWNLOADING, 100);

        for (Long songId : songIdList) {
            streamingJobDao.updateStatus(songId, JobStatus.NOT_READY);
        }
    }
}
