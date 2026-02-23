package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.service.AudioTranscodeService;
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
public class AudioTranscoderScheduler {

    private final AudioTranscodeService audioTranscodeService;
    private final SongProgressingStatusRepository songProgressingStatusRepository;

    private final Logger transcodeLog = LoggerFactory.getLogger("WORKER_TRANSCODE");

    // 매일 05시에 mp3 -> m3u8, ts 형변환 진행
    @Scheduled(cron = "0 0 5 * * ?")
    public void transcodeAudio() {

        List<Long> songIdList = songProgressingStatusRepository.findSongIdListByProgressingStatus(ProgressingStatus.READY, PageRequest.of(0, 200));

        for (Long songId : songIdList) {

            int claimedTranscode = songProgressingStatusRepository.claimStatusBySongId(songId, ProgressingStatus.READY, ProgressingStatus.TRANSCODING);

            if (claimedTranscode == 0) {
                continue;
            }

            try {
                audioTranscodeService.transcodeSong(songId);
            } catch (Exception exception) {
                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.TRANSCODE_FAILED);

                transcodeLog.error("SongId : {}, Transcode Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 매일 07시에 mp3 -> m3u8, ts 형변환 재시도
    @Scheduled(cron = "0 0 7  * * ?")
    public void retryTranscodeAudio() {

        List<Long> songIdList = songProgressingStatusRepository.findSongIdListByProgressingStatus(ProgressingStatus.TRANSCODE_FAILED, PageRequest.of(0, 200));

        for (Long songId : songIdList) {

            int claimedTranscode = songProgressingStatusRepository.claimStatusBySongId(songId, ProgressingStatus.TRANSCODE_FAILED, ProgressingStatus.TRANSCODING);

            if (claimedTranscode == 0) {
                continue;
            }

            try {
                audioTranscodeService.transcodeSong(songId);
            } catch (Exception exception) {
                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.TRANSCODE_FAILED);

                transcodeLog.error("SongId : {}, Transcode Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 매일 01시 30분에 TRANSCODING으로 상태가 멈춰 있으면 작업 가능 상태로 복구
    @Scheduled(cron = "0 30 1 * * ?")
//    @Scheduled(fixedDelay = 50000)
    public void updateStatusTranscodingToReady() {

        List<SongProgressingStatus> songProgressingStatusList = songProgressingStatusRepository.findSongProgressingStatusByProgressingStatus(ProgressingStatus.TRANSCODING, PageRequest.of(0, 100));

        for (SongProgressingStatus songProgressingStatus : songProgressingStatusList) {
            songProgressingStatus.updateStatus(ProgressingStatus.READY);
        }
    }
}
