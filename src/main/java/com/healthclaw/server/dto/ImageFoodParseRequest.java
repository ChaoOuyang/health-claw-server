package com.healthclaw.server.dto;

import lombok.Data;

@Data
public class ImageFoodParseRequest {
    private String imageBase64;
    private String mealType = "BREAKFAST";
}
