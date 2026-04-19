package com.healthclaw.server.repository;

import com.healthclaw.server.entity.UserFoodKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserFoodKnowledgeRepository extends JpaRepository<UserFoodKnowledge, Long> {

    @Query("SELECT k FROM UserFoodKnowledge k WHERE k.foodName LIKE %:keyword% ORDER BY k.sampleCount DESC")
    List<UserFoodKnowledge> findByFoodNameContaining(String keyword);

    Optional<UserFoodKnowledge> findByFoodNameAndQuantity(String foodName, String quantity);

    List<UserFoodKnowledge> findTop20ByOrderBySampleCountDesc();
}
