package com.example.worker.worker.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class RetryRequestDto {

    List<Long> songIdList;
}
