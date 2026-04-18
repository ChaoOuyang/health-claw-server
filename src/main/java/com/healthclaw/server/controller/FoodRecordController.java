package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.service.FoodRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/food")
public class FoodRecordController {

    private final FoodRecordService service;

    public FoodRecordController(FoodRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FoodRecord>> getFood(
            @RequestParam String date,
            @RequestParam(required = false) String mealType) {
        if (mealType != null && !mealType.isEmpty()) {
            return ApiResponse.ok(service.getByDateAndMeal(date, mealType));
        }
        return ApiResponse.ok(service.getByDate(date));
    }

    @PostMapping
    public ApiResponse<FoodRecord> addFood(@RequestBody FoodRecord record) {
        return ApiResponse.ok(service.save(record));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteFood(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
