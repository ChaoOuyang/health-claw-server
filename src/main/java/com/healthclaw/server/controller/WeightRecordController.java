package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.dto.WeightPredictResponse;
import com.healthclaw.server.entity.WeightRecord;
import com.healthclaw.server.service.WeightPredictionService;
import com.healthclaw.server.service.WeightRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weight")
public class WeightRecordController {

    private final WeightRecordService service;
    private final WeightPredictionService predictionService;

    public WeightRecordController(WeightRecordService service, WeightPredictionService predictionService) {
        this.service = service;
        this.predictionService = predictionService;
    }

    @GetMapping
    public ApiResponse<List<WeightRecord>> getHistory(@RequestParam(defaultValue = "14") int days) {
        return ApiResponse.ok(service.getHistory(days));
    }

    @PostMapping
    public ApiResponse<WeightRecord> saveWeight(@RequestBody WeightRecord record) {
        return ApiResponse.ok(service.upsert(record));
    }

    @GetMapping("/predict")
    public ApiResponse<WeightPredictResponse> predict() {
        List<WeightRecord> recent = service.getRecentForPrediction();
        return ApiResponse.ok(predictionService.predict(recent, 7));
    }
}
