package com.healthclaw.server.service;

import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.entity.UserFoodKnowledge;
import com.healthclaw.server.repository.UserFoodKnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodKnowledgeService {

    private final UserFoodKnowledgeRepository repo;

    public FoodKnowledgeService(UserFoodKnowledgeRepository repo) {
        this.repo = repo;
    }

    // 用户确认保存食物记录时调用，学习热量数据
    public void learn(FoodRecord record) {
        if (record.getFoodName() == null || record.getFoodName().isBlank()) return;
        String name = record.getFoodName().trim();
        String qty = record.getQuantity() == null ? "" : record.getQuantity().trim();

        repo.findByFoodNameAndQuantity(name, qty).ifPresentOrElse(existing -> {
            // 加权平均更新热量（新值权重较低，避免单次异常值污染）
            int newCalories = (existing.getCaloriesKcal() * existing.getSampleCount() + record.getCaloriesKcal())
                    / (existing.getSampleCount() + 1);
            double newProtein = (existing.getProtein() * existing.getSampleCount() + record.getProtein())
                    / (existing.getSampleCount() + 1);
            double newCarbs = (existing.getCarbs() * existing.getSampleCount() + record.getCarbs())
                    / (existing.getSampleCount() + 1);
            double newFat = (existing.getFat() * existing.getSampleCount() + record.getFat())
                    / (existing.getSampleCount() + 1);
            existing.setCaloriesKcal(newCalories);
            existing.setProtein(newProtein);
            existing.setCarbs(newCarbs);
            existing.setFat(newFat);
            existing.setSampleCount(existing.getSampleCount() + 1);
            existing.setLastUpdated(System.currentTimeMillis());
            repo.save(existing);
        }, () -> {
            UserFoodKnowledge k = new UserFoodKnowledge();
            k.setFoodName(name);
            k.setQuantity(qty);
            k.setCaloriesKcal(record.getCaloriesKcal());
            k.setProtein(record.getProtein());
            k.setCarbs(record.getCarbs());
            k.setFat(record.getFat());
            repo.save(k);
        });
    }

    // 从食物名列表中查找匹配的历史记录，拼成 prompt 片段
    public String buildReferenceHint(List<String> foodNames) {
        if (foodNames == null || foodNames.isEmpty()) return "";

        List<String> lines = new ArrayList<>();
        for (String name : foodNames) {
            String keyword = name.length() > 2 ? name.substring(0, name.length() / 2 + 1) : name;
            List<UserFoodKnowledge> hits = repo.findByFoodNameContaining(keyword);
            for (UserFoodKnowledge k : hits) {
                String qty = k.getQuantity().isBlank() ? "" : "(" + k.getQuantity() + ")";
                lines.add(String.format("- %s%s：%dkcal，蛋白%.1fg 碳水%.1fg 脂肪%.1fg（来自你的%d次记录）",
                        k.getFoodName(), qty, k.getCaloriesKcal(),
                        k.getProtein(), k.getCarbs(), k.getFat(), k.getSampleCount()));
            }
        }
        if (lines.isEmpty()) return "";
        return "【你的历史参考数据，优先采用】\n" + String.join("\n", lines) + "\n";
    }
}
