package com.healthclaw.server.service;

import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.repository.FoodRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodRecordService {

    private final FoodRecordRepository repo;
    private final FoodKnowledgeService knowledgeService;

    public FoodRecordService(FoodRecordRepository repo, FoodKnowledgeService knowledgeService) {
        this.repo = repo;
        this.knowledgeService = knowledgeService;
    }

    public List<FoodRecord> getByDate(String date) {
        return repo.findByRecordDateOrderByRecordTimestampAsc(date);
    }

    public List<FoodRecord> getByDateAndMeal(String date, String mealType) {
        return repo.findByRecordDateAndMealTypeOrderByRecordTimestampAsc(date, mealType);
    }

    public FoodRecord save(FoodRecord record) {
        record.setRecordTimestamp(System.currentTimeMillis());
        FoodRecord saved = repo.save(record);
        knowledgeService.learn(saved);
        return saved;
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    public int getTotalCalories(String date) {
        Integer sum = repo.sumCaloriesByDate(date);
        return sum != null ? sum : 0;
    }
}
