package com.example.batchmonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_execution_id", nullable = false)
    private BatchExecution batchExecution;

    @Column(length = 2000)
    private String message;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private LogType logType;

    public enum LogType {
        STDOUT, STDERR, SYSTEM
    }
}
