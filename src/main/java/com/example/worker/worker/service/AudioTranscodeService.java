package com.example.worker.worker.service;

import com.example.worker.common.dto.response.TranscodeResultDto;
import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.dto.RetryRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscodeService {

    private final AudioTranscoder audioTranscoder;
    private final SongDao songDao;
    private final StreamingJobDao streamingJobDao;

    public void retryTranscodeSong(RetryRequestDto requestDto) {

        List<Long> songIdList = requestDto.getSongIdList();

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
