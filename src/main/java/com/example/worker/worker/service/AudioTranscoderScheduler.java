package com.example.worker.worker.service;

import com.example.worker.common.enums.JobStatus;
import com.example.worker.common.dto.response.TranscodeResultDto;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
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

    @Scheduled(cron = "0 0 5 * * ?")
    public void transcodeAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.READY, 200);

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

    @Scheduled(cron = "0 0 7  * * ?")
    public void reTranscodeAudio() {

        List<Long> songIdList = streamingJobDao.findSongIdListByJobStatus(JobStatus.TRANSCODE_FAILED, 200);

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
}
