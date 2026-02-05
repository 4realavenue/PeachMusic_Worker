package com.example.worker.worker.controller;

import com.example.worker.worker.dto.request.WorkerTryWorkRequestDto;
import com.example.worker.worker.service.AudioTranscodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/worker")
public class TranscodeController {

    private final AudioTranscodeService audioTranscodeService;

    /**
     * API 서버에게 요청 받음
     * 선택한 음원 형 변환 로직(수동)
     */
    @PostMapping("/songs/transcode")
    public void tryTranscodeSong(
            @RequestBody WorkerTryWorkRequestDto requestDto
    ) {
        audioTranscodeService.tryTranscodeSong(requestDto);
    }
}
