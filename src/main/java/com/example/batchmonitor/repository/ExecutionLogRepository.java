package com.example.batchmonitor.repository;

import com.example.batchmonitor.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    List<ExecutionLog> findByBatchExecutionIdOrderByTimestampAsc(Long batchExecutionId);

    List<ExecutionLog> findByBatchExecutionIdAndLogTypeOrderByTimestampAsc(
            Long batchExecutionId,
            ExecutionLog.LogType logType);
}
