package com.example.batchmonitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressUpdate {

    private Long executionId;
    private Double progress;
    private String status;
}
