package com.example.worker.worker.service;

import com.example.worker.common.enums.ProgressingStatus;
import com.example.worker.common.storage.R2StorageService;
import com.example.worker.domain.song.entity.Song;
import com.example.worker.domain.song.repository.SongRepository;
import com.example.worker.domain.songprogressingstatus.repository.SongProgressingStatusRepository;
import com.example.worker.worker.dto.TranscodeResultDto;
import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.worker.AudioTranscoder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AudioTranscodeService {

    private final AudioTranscoder audioTranscoder;
    private final SongRepository songRepository;
    private final SongProgressingStatusRepository songProgressingStatusRepository;

    private final Logger transcodeLog = LoggerFactory.getLogger("WORKER_TRANSCODE");
    private final R2StorageService r2StorageService;

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

        // 임시 폴더
        String tmpDir = System.getProperty("java.io.tmpdir");
        Path localPath = Path.of(tmpDir, "peachmusic", "audio", Path.of(findSong.getAudio()).getFileName().toString());

        // 로컬 폴더에 임시 저장
        r2StorageService.download(findSong.getAudio(), localPath);

        // 형 변환
        TranscodeResultDto resultDto = audioTranscoder.transcodeAudio(localPath.toString());

        Path streamingAudioPath = Path.of(resultDto.getOutputDir());

        // R2 업로드
        String prefix = "storage/streaming/" + streamingAudioPath.getFileName();

        File[] fileList = streamingAudioPath.toFile().listFiles();

        if (fileList == null || fileList.length == 0) {
            throw new RuntimeException("파일이 존재하지 않습니다.");
        }

        Arrays.stream(fileList).filter(File::isFile).parallel().forEach(file -> {
            String fileName = file.getName();

            String contentType = resolveContentType(fileName);

            r2StorageService.publicUpload(file.toPath(), prefix + "/" + fileName, contentType);
        });

        // DB 최신화
        String masterKey = prefix + "/" + streamingAudioPath.getFileName() + ".m3u8";

        findSong.updateAudio(masterKey);

        songProgressingStatusRepository.updateStatusBySongId(songId, ProgressingStatus.SUCCESS);

        findSong.updateStatus(true);

        System.out.println("localPath=" + localPath);
        System.out.println("exists before delete=" + Files.exists(localPath));

        // 로컬 파일 + 폴더 삭제
        try {
            Files.deleteIfExists(localPath);

            File dir = streamingAudioPath.toFile();
            File[] unnecessaryFileList = dir.listFiles();

            if (unnecessaryFileList != null) {
                for (File file : unnecessaryFileList) {
                    Files.deleteIfExists(file.toPath());
                }
            }

            Files.deleteIfExists(streamingAudioPath);

        } catch (IOException ignored) {}

    }

    /**
     * 정확한 파일 타입 선언
     */
    private String resolveContentType(String fileName) {

        if (fileName.endsWith(".m3u8")) {
            return "application/vnd.apple.mpegurl";
        }

        if (fileName.endsWith(".ts")) {
            return "video/MP2T";
        }
        return "application/octet-stream";
    }
}
