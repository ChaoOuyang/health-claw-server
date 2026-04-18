package com.healthclaw.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "exercise_record", indexes = {
    @Index(columnList = "recordDate")
})
public class ExerciseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recordDate;
    private String exerciseType;
    private String exerciseName;
    private int durationMinutes;
    private int caloriesBurnedKcal;
    private String intensityLevel = "MEDIUM";

    @Column(length = 500)
    private String note = "";

    private long recordTimestamp = System.currentTimeMillis();
}
