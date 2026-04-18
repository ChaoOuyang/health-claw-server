package com.healthclaw.server.dto;

import lombok.Data;

@Data
public class FoodParseResponse {
    private String foodName;
    private String quantity;
    private int caloriesKcal;
    private int caloriesMinKcal;
    private int caloriesMaxKcal;
    private float kjValue;
    private boolean fuzzy;
    private String confidence;
    private double protein;
    private double carbs;
    private double fat;
    private String mealType = "BREAKFAST";
}
