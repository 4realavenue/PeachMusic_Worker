package com.example.worker.worker.controller;

import com.example.worker.worker.dto.WorkerRetryWorkRequestDto;
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
     * 선택한 음원 형 변환 재시작 로직(수동)
     */
    @PostMapping("/re-transcode")
    public void retryTranscodeSong(
            @RequestBody WorkerRetryWorkRequestDto requestDto
    ) {
        audioTranscodeService.tryTranscodeSong(requestDto);
    }
}
