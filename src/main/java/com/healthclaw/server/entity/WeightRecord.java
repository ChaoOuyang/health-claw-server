package com.healthclaw.server.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "weight_record", indexes = {
    @Index(columnList = "recordDate", unique = true)
})
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String recordDate;

    private float weightKg;
    private String note = "";
    private long recordTimestamp = System.currentTimeMillis();
}
