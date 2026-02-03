package com.example.worker.domain.streamingjob.dto;

import com.example.worker.common.enums.JobStatus;

public record StreamingJobDto(
        Long streamingJobId, Long songId, JobStatus jobStatus
) {}
