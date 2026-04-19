package com.healthclaw.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "user_food_knowledge", indexes = {
    @Index(columnList = "foodName")
})
public class UserFoodKnowledge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String foodName;

    @Column(length = 50)
    private String quantity;

    private int caloriesKcal;
    private double protein;
    private double carbs;
    private double fat;

    private int sampleCount = 1;
    private long lastUpdated = System.currentTimeMillis();
}
