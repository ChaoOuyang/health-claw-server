package com.healthclaw.server.repository;

import com.healthclaw.server.entity.ExerciseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    List<ExerciseRecord> findByRecordDateOrderByRecordTimestampAsc(String recordDate);

    @Query("SELECT SUM(e.caloriesBurnedKcal) FROM ExerciseRecord e WHERE e.recordDate = :date")
    Integer sumCaloriesBurnedByDate(String date);
}
