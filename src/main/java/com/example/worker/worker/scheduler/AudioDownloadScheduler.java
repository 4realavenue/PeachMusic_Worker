package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.service.AudioDownloadService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
//@ConditionalOnProperty(name="worker.bootstrap.enabled", havingValue="true")
public class AudioDownloadScheduler {

    private final SongProgressingStatusRepository songProgressingStatusRepository;
    private final AudioDownloadService audioDownloadService;

    private final static Logger downloadLog = LoggerFactory.getLogger("WORKER_DOWNLOAD");

    // 매일 04시에 url -> mp3 다운로드 진행
    @Scheduled(cron = "0 0 4 * * ?")
//    @Scheduled(fixedDelay = 50000)
    public void downloadAudio() {

        List<Long> songIdList = songProgressingStatusRepository.findSongIdListByProgressingStatus(ProgressingStatus.NOT_READY, PageRequest.of(0, 200));

        for (Long songId : songIdList) {

            int claimedDownload = songProgressingStatusRepository.claimStatusBySongId(songId, ProgressingStatus.NOT_READY, ProgressingStatus.DOWNLOADING);

            if (claimedDownload == 0) {
                continue;
            }

            try {
                audioDownloadService.downloadSong(songId);
            } catch (Exception exception) {

                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.DOWNLOAD_FAILED);

                downloadLog.error("SongId : {}, Download Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 매일 06시에 url -> mp3 다운로드 재시도
    @Scheduled(cron = "0 0 6 * * ?")
    public void retryDownloadAudio() {

        List<Long> songIdList = songProgressingStatusRepository.findSongIdListByProgressingStatus(ProgressingStatus.DOWNLOAD_FAILED, PageRequest.of(0, 200));

        for (Long songId : songIdList) {

            int claimedDownload = songProgressingStatusRepository.claimStatusBySongId(songId, ProgressingStatus.DOWNLOAD_FAILED, ProgressingStatus.DOWNLOADING);

            if (claimedDownload == 0) {
                continue;
            }

            try {
                audioDownloadService.downloadSong(songId);

            } catch (Exception exception) {
                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.DOWNLOAD_FAILED);

                downloadLog.error("SongId : {}, Download Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 매일 01시에 DOWNLOADING으로 상태가 멈춰 있으면 작업 가능 상태로 복구
    @Scheduled(cron = "0 0 1 * * ?")

//        @Scheduled(fixedDelay = 50000)
    public void updateStatusDownloadingToNotReady() {

        List<Long> songIdList = songProgressingStatusRepository.findSongIdListByProgressingStatus(ProgressingStatus.DOWNLOADING, PageRequest.of(0, 100));

        for (Long songId : songIdList) {
            SongProgressingStatus findSongProgressingStatus = songProgressingStatusRepository.findSongProgressingStatusBySong_SongId(songId)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 음원 상태 데이터 입니다"));

            findSongProgressingStatus.updateStatus(ProgressingStatus.NOT_READY);
        }
    }
}
