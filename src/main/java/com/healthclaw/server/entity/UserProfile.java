package com.healthclaw.server.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Data
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    private Integer id = 1;

    private String nickname = "";
    private String gender = "MALE";
    private int ageYears = 25;
    private float heightCm = 170f;
    private float currentWeightKg = 70f;
    private float targetWeightKg = 65f;
    private float startWeightKg = 70f;
    private int bmrKcal = -1;
    private int dailyCalorieGoalKcal = -1;
    private String targetDate = "";

    private LocalDateTime updatedAt = LocalDateTime.now();

    @Transient
    public int getEffectiveBmr() {
        if (bmrKcal > 0) return bmrKcal;
        float base = 10 * currentWeightKg + 6.25f * heightCm - 5 * ageYears;
        return (int) (gender.equals("MALE") ? base + 5 : base - 161);
    }

    @Transient
    public int getEffectiveDailyGoal() {
        if (dailyCalorieGoalKcal > 0) return dailyCalorieGoalKcal;
        // 如果设置了目标日期，根据体重差和天数计算每日缺口
        if (targetDate != null && !targetDate.isEmpty()) {
            try {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(targetDate));
                if (daysLeft > 0) {
                    float weightDiff = currentWeightKg - targetWeightKg;
                    // 1kg 脂肪 ≈ 7700 kcal
                    int totalDeficit = (int) (weightDiff * 7700);
                    int dailyDeficit = (int) (totalDeficit / daysLeft);
                    // 每日摄入 = TDEE - 缺口，TDEE = BMR * 1.2
                    int tdee = (int) (getEffectiveBmr() * 1.2f);
                    // 每日缺口上限 1000 kcal，下限不低于 BMR * 0.7（安全线）
                    dailyDeficit = Math.min(dailyDeficit, 1000);
                    return Math.max(tdee - dailyDeficit, (int) (getEffectiveBmr() * 0.7f));
                }
            } catch (Exception ignored) {}
        }
        return (int) (getEffectiveBmr() * 1.2f * 0.85f);
    }

    @Transient
    public int getDailyDeficit() {
        return (int) (getEffectiveBmr() * 1.2f) - getEffectiveDailyGoal();
    }

    @Transient
    public int getDaysToTarget() {
        if (targetDate == null || targetDate.isEmpty()) return -1;
        try {
            return (int) ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(targetDate));
        } catch (Exception e) {
            return -1;
        }
    }
}
