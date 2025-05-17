package com.example.batchmonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String scriptPath;
    private String parameters;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private Integer exitCode;
    private Double progress;
    private String outputFilePath;
    private String errorMessage;

    @OneToMany(mappedBy = "batchExecution", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ExecutionLog> logs = new ArrayList<>();

    public enum ExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, TIMED_OUT
    }
}
