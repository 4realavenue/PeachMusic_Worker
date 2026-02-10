package com.example.worker.worker.service;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.song.repository.SongRepository;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.dto.TranscodeResultDto;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.worker.AudioTranscoder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioTranscodeService {

    private final AudioTranscoder audioTranscoder;
    private final SongRepository songRepository;
    private final SongProgressingStatusRepository songProgressingStatusRepository;

    private final Logger transcodeLog = LoggerFactory.getLogger("WORKER_TRANSCODE");

    // 음원 형변환 시도 (관리자 수동)
    @Transactional
    public void tryTranscodeSong(WorkerTryWorkRequestDto requestDto) {

        List<ProgressingStatus> allowed = List.of(ProgressingStatus.READY, ProgressingStatus.TRANSCODE_FAILED);

        for (Long songId : requestDto.getSongIdList()) {

            int claimedTranscode = songProgressingStatusRepository.readyToAbleWork(songId, allowed, ProgressingStatus.TRANSCODING);

            if (claimedTranscode == 0) {
                continue;
            }

            try {
                transcodeSong(songId);
            } catch (Exception exception) {
                songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.TRANSCODE_FAILED);

                transcodeLog.error("SongId : {}, Transcode Failed : {}", songId, exception.getMessage());
            }
        }
    }

    // 음원 형변환 수행
    @Transactional
    public void transcodeSong(Long songId) {

        Song findSong = songRepository.findSongBySongId(songId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 음원입니다"));

        SongProgressingStatus findSongProgressingStatus = songProgressingStatusRepository.findSongProgressingStatusBySong_SongId(songId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 데이터입니다"));

        TranscodeResultDto transcodeResult = audioTranscoder.transcodeAudio(findSong.getAudio());

        findSong.updateAudio(transcodeResult.getPlaylistPath());

        findSongProgressingStatus.updateStatus(ProgressingStatus.SUCCESS);

        findSong.updateStatus(true);

    }
}
