package com.healthclaw.server.agent;

import lombok.Data;

@Data
public class IntentResult {
    // FOOD_RECORD | EXERCISE_RECORD | FOOD_AND_EXERCISE | WEIGHT_RECORD | QUERY_TODAY | DAILY_COMMENT | UNKNOWN
    private String intent;
    private String foodText;
    private String exerciseText;
    private String mealType;   // BREAKFAST | LUNCH | DINNER | SNACK
    private Double weightKg;
    private String rawMessage; // 原始用户输入，兜底用
}
