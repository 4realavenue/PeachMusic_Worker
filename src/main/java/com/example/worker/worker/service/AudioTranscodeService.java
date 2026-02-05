package com.example.worker.worker.service;

import com.example.worker.common.dto.response.TranscodeResultDto;
import com.example.worker.common.enums.JobStatus;
import com.example.worker.domain.song.repository.SongDao;
import com.example.worker.domain.streamingjob.repository.StreamingJobDao;
import com.example.worker.worker.AudioTranscoder;
import com.example.worker.worker.dto.WorkerRetryWorkRequestDto;
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

    // 음원 형변환 시도 (관리자 수동)
    public void tryTranscodeSong(WorkerRetryWorkRequestDto requestDto) {

        List<Long> songIdList = requestDto.getSongIdList();

        for (Long songId : songIdList) {

            boolean claimed = streamingJobDao.claimStatus(songId, JobStatus.READY, JobStatus.TRANSCODING) || streamingJobDao.claimStatus(songId, JobStatus.TRANSCODE_FAILED, JobStatus.TRANSCODING);

            if (!claimed) continue;

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
