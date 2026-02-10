package com.example.worker.worker.controller;

import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.service.AudioDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/worker")
public class DownloadController {

    private final AudioDownloadService audioDownloadService;

    /**
     * API 서버에게 요청 받음
     * 선택한 음원 다운로드 로직(수동)
     */
    @PostMapping("/songs/download-request")
    public void tryDownloadSong(
            @RequestBody WorkerTryWorkRequestDto requestDto
    ) {
        audioDownloadService.tryDownloadSong(requestDto);
    }
}
