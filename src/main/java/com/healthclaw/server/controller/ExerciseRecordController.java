package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.entity.ExerciseRecord;
import com.healthclaw.server.service.ExerciseRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercise")
public class ExerciseRecordController {

    private final ExerciseRecordService service;

    public ExerciseRecordController(ExerciseRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ExerciseRecord>> getExercise(@RequestParam String date) {
        return ApiResponse.ok(service.getByDate(date));
    }

    @PostMapping
    public ApiResponse<ExerciseRecord> addExercise(@RequestBody ExerciseRecord record) {
        return ApiResponse.ok(service.save(record));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteExercise(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}
