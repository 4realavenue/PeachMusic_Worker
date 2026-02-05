package com.example.worker.common.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TranscodeResultDto {

    // .m3u8
    public final String playlistPath;
    // 저장 경로
    public final String outputDir;
}
