package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.service.AudioTranscodeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AudioTranscodeSchedulerTest {

    // 1) 중복 작업 방지: claim 성공한 것만 transcodeSong 호출
    @Test
    void transcodeAudio_claim성공한것만_transcodeSong호출() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioTranscodeService service = mock(AudioTranscodeService.class);
        AudioTranscoderScheduler scheduler = new AudioTranscoderScheduler(service, repo);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.READY), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        when(repo.claimStatusBySongId(1L, ProgressingStatus.READY, ProgressingStatus.TRANSCODING)).thenReturn(1);
        when(repo.claimStatusBySongId(2L, ProgressingStatus.READY, ProgressingStatus.TRANSCODING)).thenReturn(0);
        when(repo.claimStatusBySongId(3L, ProgressingStatus.READY, ProgressingStatus.TRANSCODING)).thenReturn(1);

        scheduler.transcodeAudio();

        verify(service).transcodeSong(1L);
        verify(service, never()).transcodeSong(2L);
        verify(service).transcodeSong(3L);
    }

    // 2) 대상 선정: READY로 조회 + PageRequest size=200
    @Test
    void transcodeAudio_READY로조회하고_PageSize200이다() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioTranscodeService service = mock(AudioTranscodeService.class);
        AudioTranscoderScheduler scheduler = new AudioTranscoderScheduler(service, repo);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.READY), any()))
                .thenReturn(List.of());

        scheduler.transcodeAudio();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findSongIdListByProgressingStatus(eq(ProgressingStatus.READY), captor.capture());

        Pageable p = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, p.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(200, p.getPageSize());
    }

    // 3) 예외가 나도 스케줄러 전체가 안 죽고 다음 곡 계속 + 실패 상태 업데이트
    @Test
    void transcodeAudio_중간에예외나도_다음곡계속_실패상태업데이트() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioTranscodeService service = mock(AudioTranscodeService.class);
        AudioTranscoderScheduler scheduler = new AudioTranscoderScheduler(service, repo);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.READY), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        when(repo.claimStatusBySongId(anyLong(), eq(ProgressingStatus.READY), eq(ProgressingStatus.TRANSCODING)))
                .thenReturn(1);

        doThrow(new RuntimeException("ffmpeg fail")).when(service).transcodeSong(2L);

        assertDoesNotThrow(scheduler::transcodeAudio);

        verify(service).transcodeSong(1L);
        verify(service).transcodeSong(2L);
        verify(service).transcodeSong(3L);

        verify(repo).updateStatusBySongId(2L, ProgressingStatus.TRANSCODE_FAILED);
    }
}
