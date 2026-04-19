package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.dto.DailyCommentRequest;
import com.healthclaw.server.dto.ExerciseParseResponse;
import com.healthclaw.server.dto.FoodParseRequest;
import com.healthclaw.server.dto.FoodParseResponse;
import com.healthclaw.server.dto.ImageFoodParseRequest;
import java.util.List;
import com.healthclaw.server.entity.UserProfile;
import com.healthclaw.server.service.AiService;
import com.healthclaw.server.service.FoodKnowledgeService;
import com.healthclaw.server.service.UserProfileService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final UserProfileService profileService;
    private final FoodKnowledgeService knowledgeService;

    public AiController(AiService aiService, UserProfileService profileService, FoodKnowledgeService knowledgeService) {
        this.aiService = aiService;
        this.profileService = profileService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/parse-food")
    public ApiResponse<List<FoodParseResponse>> parseFood(@RequestBody FoodParseRequest req) {
        try {
            List<String> keywords = extractKeywords(req.getUserInput());
            String hint = knowledgeService.buildReferenceHint(keywords);
            return ApiResponse.ok(aiService.parseFoodList(req.getUserInput(), hint));
        } catch (Exception e) {
            return ApiResponse.error("AI 解析失败: " + e.getMessage());
        }
    }

    private List<String> extractKeywords(String input) {
        // 按常见分隔符切词，过滤短词，作为查库关键词
        return Arrays.stream(input.split("[，,、和跟还有加上\\s]+"))
                .map(String::trim)
                .filter(s -> s.length() >= 2)
                .collect(Collectors.toList());
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

    @PostMapping("/parse-food-image")
    public ApiResponse<List<FoodParseResponse>> parseFoodFromImage(@RequestBody ImageFoodParseRequest req) {
        try {
            return ApiResponse.ok(aiService.parseFoodFromImage(req.getImageBase64(), req.getMealType()));
        } catch (Exception e) {
            return ApiResponse.error("图片识别失败: " + e.getMessage());
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
