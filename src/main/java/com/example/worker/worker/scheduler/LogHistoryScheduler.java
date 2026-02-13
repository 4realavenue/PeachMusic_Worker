package com.example.worker.worker.scheduler;

import com.example.worker.common.storage.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogHistoryScheduler {

    private final R2StorageService r2StorageService;

    @Value("${logging.file.path:${java.io.tmpdir}/peachmusic-worker/logs}")
    private String logPath;

    // 매일 00시 30분에 어제자 로그 기록 R2에 보관
    @Scheduled(cron = "0 10 0 * * ?")
//    @Scheduled(fixedDelay = 5000)
    public void archiveLogScheduler() {

        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Path downloadLogPath = Path.of(logPath, "download", "error." + yesterday + ".log");
        Path transcodeLogPath = Path.of(logPath, "transcode", "error." + yesterday + ".log");
        Path generalLogPath = Path.of(logPath, "general", "error." + yesterday + ".log");

        archiveLog(downloadLogPath, "storage/logs/download/error." + yesterday + "_log.txt");
        archiveLog(transcodeLogPath, "storage/logs/transcode/error." + yesterday + "_log.txt");
        archiveLog(generalLogPath, "storage/logs/general/error." + yesterday + "_log.txt");

    }

    // 로그 파일 업로드 후 삭제
    private void archiveLog(Path logPath, String key) {

        try {
            if (!Files.exists(logPath) || Files.isDirectory(logPath)) {
                return;
            }

            r2StorageService.privateUpload(logPath, key, "text/plain");

            Files.deleteIfExists(logPath);

        } catch (IOException exception) {
            log.error("로그 보관에 실패했습니다. :" + exception.getMessage());
        }
    }

}

