package com.example.worker.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {

    // url만 있는 상태
    NOT_READY,

    // url -> mp3 다운로드 작업 중
    DOWNLOADING,

    // url -> mp3 다운로드 실패
    DOWNLOAD_FAILED,

    // mp3만 있는 상태
    READY,

    // mp3 -> m3u8, ts 형 변환 중
    TRANSCODING,

    // mp3 -> m3u8, ts 형 변환 실패
    TRANSCODE_FAILED,

    // 형 변환 완료
    SUCCESS

    ;
}