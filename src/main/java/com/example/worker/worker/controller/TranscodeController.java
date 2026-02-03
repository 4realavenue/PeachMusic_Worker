package com.example.worker.worker.controller;

import com.example.worker.worker.dto.RetryRequestDto;
import com.example.worker.worker.service.AudioTranscodeService;
import com.example.worker.worker.service.AudioTranscoder;
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

    @PostMapping("/re-transcode")
    public void retryTranscodeSong(
            @RequestBody RetryRequestDto requestDto
            ) {
        audioTranscodeService.retryTranscodeSong(requestDto);
    }


}
