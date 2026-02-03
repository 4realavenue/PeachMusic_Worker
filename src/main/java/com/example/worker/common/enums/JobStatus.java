package com.example.worker.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {

    NOT_READY,

    DOWNLOADING,

    DOWNLOAD_FAILED,

    READY,

    TRANSCODING,

    TRANSCODE_FAILED,

    SUCCESS

    ;
}