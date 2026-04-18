package com.healthclaw.server.dto;

import lombok.Data;
import java.util.List;

@Data
public class WeightPredictResponse {
    private List<WeightPoint> actual;
    private List<WeightPoint> predicted;

    @Data
    public static class WeightPoint {
        private String date;
        private float weightKg;

        public WeightPoint(String date, float weightKg) {
            this.date = date;
            this.weightKg = weightKg;
        }
    }
}
