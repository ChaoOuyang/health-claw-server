package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.dto.DailyCommentRequest;
import com.healthclaw.server.dto.ExerciseParseResponse;
import com.healthclaw.server.dto.FoodParseRequest;
import com.healthclaw.server.dto.FoodParseResponse;
import java.util.List;
import com.healthclaw.server.entity.UserProfile;
import com.healthclaw.server.service.AiService;
import com.healthclaw.server.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserProfileService profileService;

    public AiController(AiService aiService, UserProfileService profileService) {
        this.aiService = aiService;
        this.profileService = profileService;
    }

    @PostMapping("/parse-food")
    public ApiResponse<List<FoodParseResponse>> parseFood(@RequestBody FoodParseRequest req) {
        try {
            return ApiResponse.ok(aiService.parseFoodList(req.getUserInput()));
        } catch (Exception e) {
            return ApiResponse.error("AI 解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/parse-exercise")
    public ApiResponse<List<ExerciseParseResponse>> parseExercise(@RequestBody FoodParseRequest req) {
        try {
            return ApiResponse.ok(aiService.parseExerciseList(req.getUserInput()));
        } catch (Exception e) {
            return ApiResponse.error("AI 解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/daily-comment")
    public ApiResponse<String> dailyComment(@RequestBody DailyCommentRequest req) {
        try {
            return ApiResponse.ok(aiService.generateDailyComment(req));
        } catch (Exception e) {
            return ApiResponse.error("生成点评失败: " + e.getMessage());
        }
    }

    @GetMapping("/recommend-meal")
    public ApiResponse<String> recommendMeal() {
        try {
            UserProfile profile = profileService.getProfile();
            return ApiResponse.ok(aiService.recommendMeal(profile));
        } catch (Exception e) {
            return ApiResponse.error("生成食谱失败: " + e.getMessage());
        }
    }

    @GetMapping("/recommend-exercise")
    public ApiResponse<String> recommendExercise() {
        try {
            UserProfile profile = profileService.getProfile();
            return ApiResponse.ok(aiService.recommendExercise(profile));
        } catch (Exception e) {
            return ApiResponse.error("生成运动推荐失败: " + e.getMessage());
        }
    }
}
