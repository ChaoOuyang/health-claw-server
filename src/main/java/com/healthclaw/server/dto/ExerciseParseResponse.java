package com.healthclaw.server.dto;

import lombok.Data;

@Data
public class ExerciseParseResponse {
    private String exerciseName;
    private int durationMinutes;
    private int caloriesBurnedKcal;
    private String intensityLevel = "MEDIUM";
}
