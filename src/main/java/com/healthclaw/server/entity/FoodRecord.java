package com.healthclaw.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "food_record", indexes = {
    @Index(columnList = "recordDate,mealType")
})
public class FoodRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recordDate;
    private String mealType;

    @Column(length = 500)
    private String rawInputText;

    private String foodName;
    private String quantity;
    private int caloriesKcal;
    private int caloriesMinKcal;
    private int caloriesMaxKcal;
    private boolean fuzzy;
    private float kjValue;
    private double protein;
    private double carbs;
    private double fat;
    private long recordTimestamp = System.currentTimeMillis();
}
