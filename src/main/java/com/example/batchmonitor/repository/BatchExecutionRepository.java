package com.example.batchmonitor.repository;

import com.example.batchmonitor.entity.BatchExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchExecutionRepository extends JpaRepository<BatchExecution, Long> {

    List<BatchExecution> findAllByOrderByStartTimeDesc();

    List<BatchExecution> findByStatus(BatchExecution.ExecutionStatus status);
}
