package com.example.worker.worker.worker;

import com.example.worker.worker.dto.TranscodeResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class AudioTranscoder {

    @Value("${worker.media.streaming-audio-path}")
    private String streamingAudioPath;

    /**
     * 형 변환 로직
     */
    public TranscodeResultDto transcodeAudio(String audioPath) {

        try {
            Path input = resolveInputFile(audioPath);

            if (!Files.exists(input)) {
                throw new IllegalArgumentException("원본 mp3가 없습니다: " + input);
            }

            String audioFileName = input.getFileName().toString().replaceFirst("\\.mp3$", "");

            Path outputPath = Path.of(streamingAudioPath, audioFileName);
            Files.createDirectories(outputPath);

            Path streamingAudio = outputPath.resolve(audioFileName + ".m3u8");

            Path streamingSegment = outputPath.resolve(audioFileName + "_%04d.ts");

            List<String> cmd = List.of(
                    "ffmpeg", "-y", "-i", input.toString(), "-vn",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-ac", "2",
                    "-ar", "44100",
                    "-f", "hls",
                    "-hls_time", "6",
                    "-hls_playlist_type", "vod",
                    "-hls_flags", "independent_segments",
                    "-hls_segment_filename", streamingSegment.toString(), streamingAudio.toString()
            );

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder log = new StringBuilder();

            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line).append("\n");
                }
            }

            boolean finish = process.waitFor(10, TimeUnit.MINUTES);

            if (!finish) {
                process.destroyForcibly();
                throw new RuntimeException("ffmpeg 시간 초과");
            }

            int exit = process.exitValue();

            if (exit != 0) {
                throw new RuntimeException("ffmpeg 실패");
            }

            if (!Files.exists(streamingAudio)) {
                throw new RuntimeException("m3u8가 생성되지 않았습니다: " + streamingAudio);
            }

            return new TranscodeResultDto(streamingAudio.toString(), outputPath.toString());

        } catch (Exception exception) {
            throw new RuntimeException("HLS 변환 실패 : " + exception.getMessage());
        }
    }

    /**
     * 경로 통일화를 위한 로직
     */
    private Path resolveInputFile(String audioPath) {

        if (audioPath == null || audioPath.isBlank()) {
            throw new IllegalArgumentException("audioPath가 비어있습니다.");
        }

        Path validPath = Path.of(audioPath);

        if (validPath.isAbsolute()) {
            return validPath;
        }

        throw new IllegalArgumentException("지원하지 않는 audioPath 형식: " + audioPath);
    }
}
