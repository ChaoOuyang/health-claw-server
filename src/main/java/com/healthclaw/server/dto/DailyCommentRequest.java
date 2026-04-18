package com.healthclaw.server.dto;

import lombok.Data;

@Data
public class DailyCommentRequest {
    private int intakeKcal;
    private int burnedKcal;
    private int bmrKcal;
    private float weightKg;
    private float targetWeightKg;
    private String date;
}
