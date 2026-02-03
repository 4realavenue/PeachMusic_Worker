package com.example.worker.common.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TranscodeResultDto {

    public final String playlistPath;
    public final String outputDir;
}
