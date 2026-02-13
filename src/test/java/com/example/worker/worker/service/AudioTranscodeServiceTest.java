package com.example.worker.worker.service;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.common.storage.R2StorageService;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.song.repository.SongRepository;
import com.example.worker.domain.songprogressingstatus.entity.SongProgressingStatus;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.dto.TranscodeResultDto;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.worker.AudioTranscoder;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AudioTranscodeServiceTest {

    @Test
    void transcodeSong_실패하면_예외던진다_상태변경은호출자책임() {
        AudioTranscoder transcoder = mock(AudioTranscoder.class);
        SongRepository songRepo = mock(SongRepository.class);
        SongProgressingStatusRepository statusRepo = mock(SongProgressingStatusRepository.class);
        R2StorageService r2Service = mock(R2StorageService.class);

        AudioTranscodeService service = new AudioTranscodeService(transcoder, songRepo, statusRepo, r2Service);

        Long songId = 1L;

        Song song = mock(Song.class);
        when(song.getAudio()).thenReturn("uploads/audios/a.mp3");
        when(songRepo.findSongBySongId(songId)).thenReturn(Optional.of(song));

        SongProgressingStatus sps = mock(SongProgressingStatus.class);
        when(statusRepo.findSongProgressingStatusBySong_SongId(songId)).thenReturn(Optional.of(sps));

        when(transcoder.transcodeAudio(anyString()))
                .thenThrow(new RuntimeException("ffmpeg fail"));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> service.transcodeSong(songId));

        verify(sps, never()).updateStatus(ProgressingStatus.TRANSCODE_FAILED);
        verify(sps, never()).updateStatus(ProgressingStatus.SUCCESS);
        verify(song, never()).updateStatus(true);
    }

    @Test
    void tryTranscodeSong_claim성공한것만_작업시도하고_실패시_TRANSCODE_FAILED로업데이트() throws Exception {
        AudioTranscoder transcoder = mock(AudioTranscoder.class);
        SongRepository songRepo = mock(SongRepository.class);
        SongProgressingStatusRepository statusRepo = mock(SongProgressingStatusRepository.class);
        R2StorageService r2Service = mock(R2StorageService.class);

        AudioTranscodeService serviceSpy = spy(new AudioTranscodeService(transcoder, songRepo, statusRepo, r2Service));

        WorkerTryWorkRequestDto req = new WorkerTryWorkRequestDto();
        java.lang.reflect.Field f = WorkerTryWorkRequestDto.class.getDeclaredField("songIdList");
        f.setAccessible(true);
        f.set(req, List.of(1L, 2L, 3L));

        when(statusRepo.readyToAbleWork(eq(1L), anyList(), eq(ProgressingStatus.TRANSCODING))).thenReturn(1);
        when(statusRepo.readyToAbleWork(eq(2L), anyList(), eq(ProgressingStatus.TRANSCODING))).thenReturn(0);
        when(statusRepo.readyToAbleWork(eq(3L), anyList(), eq(ProgressingStatus.TRANSCODING))).thenReturn(1);

        doThrow(new RuntimeException("boom")).when(serviceSpy).transcodeSong(3L);

        serviceSpy.tryTranscodeSong(req);

        verify(serviceSpy).transcodeSong(1L);
        verify(serviceSpy, never()).transcodeSong(2L);
        verify(serviceSpy).transcodeSong(3L);

        verify(statusRepo).updateStatusBySongId(3L, ProgressingStatus.TRANSCODE_FAILED);
    }
}
