package com.example.worker.worker.scheduler;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.service.AudioDownloadService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AudioDownloadSchedulerTest {

    // 1) 중복 방지: claim 성공한 것만 호출
    @Test
    void downloadAudio_claim성공한것만_downloadSong호출() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioDownloadService service = mock(AudioDownloadService.class);
        AudioDownloadScheduler scheduler = new AudioDownloadScheduler(repo, service);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.NOT_READY), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        when(repo.claimStatusBySongId(1L, ProgressingStatus.NOT_READY, ProgressingStatus.DOWNLOADING)).thenReturn(1);
        when(repo.claimStatusBySongId(2L, ProgressingStatus.NOT_READY, ProgressingStatus.DOWNLOADING)).thenReturn(0);
        when(repo.claimStatusBySongId(3L, ProgressingStatus.NOT_READY, ProgressingStatus.DOWNLOADING)).thenReturn(1);

        scheduler.downloadAudio();

        verify(service).downloadSong(1L);
        verify(service, never()).downloadSong(2L);
        verify(service).downloadSong(3L);
    }

    // 2) 대상 선정: NOT_READY로 조회 + pageable size=200
    @Test
    void downloadAudio_NOT_READY로조회하고_PageSize200이다() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioDownloadService service = mock(AudioDownloadService.class);
        AudioDownloadScheduler scheduler = new AudioDownloadScheduler(repo, service);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.NOT_READY), any()))
                .thenReturn(List.of());

        scheduler.downloadAudio();

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repo).findSongIdListByProgressingStatus(eq(ProgressingStatus.NOT_READY), captor.capture());

        Pageable p = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(0, p.getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(200, p.getPageSize());
    }

    // 3) 예외가 나도 Worker가 다운되지 않고 다음 곡 작업 계속
    @Test
    void downloadAudio_중간에예외나도_다음곡은계속처리한다() {
        SongProgressingStatusRepository repo = mock(SongProgressingStatusRepository.class);
        AudioDownloadService service = mock(AudioDownloadService.class);
        AudioDownloadScheduler scheduler = new AudioDownloadScheduler(repo, service);

        when(repo.findSongIdListByProgressingStatus(eq(ProgressingStatus.NOT_READY), any()))
                .thenReturn(List.of(1L, 2L, 3L));

        when(repo.claimStatusBySongId(anyLong(), eq(ProgressingStatus.NOT_READY), eq(ProgressingStatus.DOWNLOADING)))
                .thenReturn(1);

        doThrow(new RuntimeException("boom")).when(service).downloadSong(2L);

        assertDoesNotThrow(scheduler::downloadAudio);

        verify(service).downloadSong(1L);
        verify(service).downloadSong(2L);
        verify(service).downloadSong(3L);

        // 예외난 곡은 실패 처리까지 했는지도 확인(현재 코드 기준)
        verify(repo).updateStatusBySongId(2L, ProgressingStatus.DOWNLOAD_FAILED);
    }
}
