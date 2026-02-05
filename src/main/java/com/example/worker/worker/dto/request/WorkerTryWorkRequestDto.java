package com.example.worker.worker.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WorkerTryWorkRequestDto {

    List<Long> songIdList;
}
