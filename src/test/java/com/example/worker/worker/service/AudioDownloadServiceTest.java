package com.example.worker.worker.service;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.domain.song.repository.SongRepository;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.worker.AudioDownloader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AudioDownloadServiceTest {

    @Test
    void downloadSong_성공시_audio업데이트_상태READY() {
        SongRepository songRepo = mock(SongRepository.class);
        SongProgressingStatusRepository statusRepo = mock(SongProgressingStatusRepository.class);

        AudioDownloadService service = new AudioDownloadService(songRepo, statusRepo);

        Long songId = 1L;

        // Song은 mock으로 만들면 createdAt null 이슈가 날 수 있음 (SongFileNamePolicy가 createdAt을 씀)
        // 그래서 spy + createdAt stub을 권장. (Song.getCreatedAt()을 stub)
        Song song = mock(Song.class);
        when(song.getAudio()).thenReturn("https://example.com/a.mp3");
        when(song.getName()).thenReturn("Hello");
        // createdAt이 null이면 SongFileNamePolicy에서 NPE가 나므로 꼭 stub
        when(song.getCreatedAt()).thenReturn(java.time.LocalDateTime.of(2026, 2, 9, 0, 0));
        when(songRepo.findSongBySongId(songId)).thenReturn(Optional.of(song));

        SongProgressingStatus sps = mock(SongProgressingStatus.class);
        when(statusRepo.findBySong_SongId(songId)).thenReturn(Optional.of(sps));

        try (MockedStatic<AudioDownloader> downloaderMock = mockStatic(AudioDownloader.class)) {
            downloaderMock.when(() -> AudioDownloader.downloadAudio(anyString(), any()))
                    .thenReturn(java.nio.file.Path.of("uploads/audios/dummy.mp3"));

            service.downloadSong(songId);

            verify(song).updateAudio(startsWith("uploads/audios/"));
            verify(sps).updateStatus(ProgressingStatus.READY);
        }
    }

    @Test
    void downloadSong_다운로드실패하면_예외던진다_상태변경은호출자책임() {
        SongRepository songRepo = mock(SongRepository.class);
        SongProgressingStatusRepository statusRepo = mock(SongProgressingStatusRepository.class);
        AudioDownloadService service = new AudioDownloadService(songRepo, statusRepo);

        Long songId = 1L;

        Song song = mock(Song.class);
        when(song.getAudio()).thenReturn("https://example.com/a.mp3");
        when(song.getName()).thenReturn("Hello");
        when(song.getCreatedAt()).thenReturn(java.time.LocalDateTime.of(2026, 2, 9, 0, 0));
        when(songRepo.findSongBySongId(songId)).thenReturn(Optional.of(song));

        SongProgressingStatus sps = mock(SongProgressingStatus.class);
        when(statusRepo.findBySong_SongId(songId)).thenReturn(Optional.of(sps));

        try (MockedStatic<AudioDownloader> downloaderMock = mockStatic(AudioDownloader.class)) {
            downloaderMock.when(() -> AudioDownloader.downloadAudio(anyString(), any()))
                    .thenThrow(new RuntimeException("download fail"));

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.downloadSong(songId));

            // downloadSong은 실패 시 READY/FAILED 처리 안 함 (네 철학: 호출자에서 처리)
            verify(sps, never()).updateStatus(ProgressingStatus.READY);
            verify(sps, never()).updateStatus(ProgressingStatus.DOWNLOAD_FAILED);
        }
    }

    @Test
    void tryDownloadSong_claim성공한것만_작업시도하고_실패시_DOWNLOAD_FAILED로업데이트() throws Exception {
        SongRepository songRepo = mock(SongRepository.class);
        SongProgressingStatusRepository statusRepo = mock(SongProgressingStatusRepository.class);
        AudioDownloadService serviceSpy = spy(new AudioDownloadService(songRepo, statusRepo));

        // requestDto에 songIdList 세팅(필드가 package-private + setter가 없어서 리플렉션으로 세팅)
        WorkerTryWorkRequestDto req = new WorkerTryWorkRequestDto();
        java.lang.reflect.Field f = WorkerTryWorkRequestDto.class.getDeclaredField("songIdList");
        f.setAccessible(true);
        f.set(req, List.of(1L, 2L, 3L));

        // claim: 1,3 성공 / 2 실패
        when(statusRepo.readyToAbleWork(eq(1L), anyList(), eq(ProgressingStatus.DOWNLOADING))).thenReturn(1);
        when(statusRepo.readyToAbleWork(eq(2L), anyList(), eq(ProgressingStatus.DOWNLOADING))).thenReturn(0);
        when(statusRepo.readyToAbleWork(eq(3L), anyList(), eq(ProgressingStatus.DOWNLOADING))).thenReturn(1);

        // 3번에서 downloadSong이 실패(예외 던짐)하도록
        doThrow(new RuntimeException("boom")).when(serviceSpy).downloadSong(3L);

        serviceSpy.tryDownloadSong(req);

        verify(serviceSpy).downloadSong(1L);
        verify(serviceSpy, never()).downloadSong(2L);
        verify(serviceSpy).downloadSong(3L);

        // 실패한 3번은 호출자가 DOWNLOAD_FAILED로 업데이트
        verify(statusRepo).updateStatusBySongId(3L, ProgressingStatus.DOWNLOAD_FAILED);
    }
}
