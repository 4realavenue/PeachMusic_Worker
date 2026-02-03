package com.example.worker.worker.controller;

import com.example.worker.worker.dto.RetryRequestDto;
import com.example.worker.worker.service.AudioDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/worker")
public class DownloadController {

    private final AudioDownloadService audioDownloadService;

    @PostMapping("/re-download")
    public void retryDownloadSong(
            @RequestBody RetryRequestDto requestDto
            ) {
        audioDownloadService.retryDownloadSong(requestDto);
    }
}
