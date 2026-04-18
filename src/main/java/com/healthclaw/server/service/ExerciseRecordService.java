package com.healthclaw.server.service;

import com.healthclaw.server.entity.ExerciseRecord;
import com.healthclaw.server.repository.ExerciseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseRecordService {

    private final ExerciseRecordRepository repo;

    public ExerciseRecordService(ExerciseRecordRepository repo) {
        this.repo = repo;
    }

    public List<ExerciseRecord> getByDate(String date) {
        return repo.findByRecordDateOrderByRecordTimestampAsc(date);
    }

    public ExerciseRecord save(ExerciseRecord record) {
        record.setRecordTimestamp(System.currentTimeMillis());
        return repo.save(record);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public int getTotalBurned(String date) {
        Integer sum = repo.sumCaloriesBurnedByDate(date);
        return sum != null ? sum : 0;
    }
}
