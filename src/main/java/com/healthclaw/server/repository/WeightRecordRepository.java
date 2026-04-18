package com.healthclaw.server.repository;

import com.healthclaw.server.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
    Optional<WeightRecord> findByRecordDate(String recordDate);
    List<WeightRecord> findByRecordDateBetweenOrderByRecordDateAsc(String startDate, String endDate);
    List<WeightRecord> findTop14ByOrderByRecordDateDesc();
}
