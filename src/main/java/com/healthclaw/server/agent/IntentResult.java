package com.healthclaw.server.agent;

import lombok.Data;
import java.util.List;

@Data
public class IntentResult {
    // tags 取值：FOOD | EXERCISE | WEIGHT | DELETE_FOOD | DELETE_EXERCISE | QUERY_TODAY | DAILY_COMMENT | UNKNOWN
    private List<String> tags;
    private String foodText;
    private String exerciseText;
    private String mealType;   // BREAKFAST | LUNCH | DINNER | SNACK
    private Double weightKg;
    private String deleteKeyword;
    private String rawMessage;
}
