package com.healthclaw.server.repository;

import com.healthclaw.server.entity.FoodRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FoodRecordRepository extends JpaRepository<FoodRecord, Long> {
    List<FoodRecord> findByRecordDateOrderByRecordTimestampAsc(String recordDate);
    List<FoodRecord> findByRecordDateAndMealTypeOrderByRecordTimestampAsc(String recordDate, String mealType);

    @Query("SELECT SUM(f.caloriesKcal) FROM FoodRecord f WHERE f.recordDate = :date")
    Integer sumCaloriesByDate(String date);
}
