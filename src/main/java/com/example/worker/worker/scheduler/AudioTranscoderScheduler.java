package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.JobStatus;
import com.example.worker.common.dto.response.TranscodeResultDto;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.AudioTranscoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscoderScheduler {

    private final SongDao songDao;
    private final StreamingJobDao streamingJobDao;
    private final AudioTranscoder audioTranscoder;

    // 매일 05시에 mp3 -> m3u8, ts 형변환 진행
    @Scheduled(cron = "0 0 5 * * ?")
    public void transcodeAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.READY, 300);

        for (Long songId : songIdList) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.READY, JobStatus.TRANSCODING)) continue;

            try {
                TranscodeResultDto transcodeResult = audioTranscoder.transcodeAudio(songDao.loadMetaData(songId).audio());

                songDao.updateAudioPath(songId, transcodeResult.getPlaylistPath());

                streamingJobDao.updateStatus(songId, JobStatus.SUCCESS);

                songDao.updateStatus(songId, true);
            } catch (Exception exception) {
                streamingJobDao.updateStatus(songId, JobStatus.TRANSCODE_FAILED);
                log.error("SongId : {}, Transcode Failed : {}",songId, exception.getMessage());
            }
        }
    }

    // 매일 07시에 mp3 -> m3u8, ts 형변환 재시도
    @Scheduled(cron = "0 0 7  * * ?")
    public void reTranscodeAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.TRANSCODE_FAILED, 300);

        for (Long songId : songIdList) {
            if (!streamingJobDao.claimStatus(songId, JobStatus.TRANSCODE_FAILED, JobStatus.TRANSCODING)) continue;

            try {
                TranscodeResultDto transcodeResult = audioTranscoder.transcodeAudio(songDao.loadMetaData(songId).audio());

                songDao.updateAudioPath(songId, transcodeResult.getPlaylistPath());

                streamingJobDao.updateStatus(songId, JobStatus.SUCCESS);

                songDao.updateStatus(songId, true);
            } catch (Exception exception) {
                streamingJobDao.updateStatus(songId, JobStatus.TRANSCODE_FAILED);
                log.error("SongId : {}, Transcode Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 매일 01시 30분에 TRANSCODING으로 상태가 멈춰 있으면 작업 가능 상태로 복구
    @Scheduled(cron = "0 30 1 * * ?")
//    @Scheduled(fixedDelay = 50000)
    public void StatusTranscodingToReady() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.TRANSCODING, 100);

        for (Long songId : songIdList) {
                streamingJobDao.updateStatus(songId, JobStatus.READY);
        }
    }
}
