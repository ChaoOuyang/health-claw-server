package com.healthclaw.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthclaw.server.dto.DailyCommentRequest;
import com.healthclaw.server.dto.ExerciseParseResponse;
import com.healthclaw.server.dto.FoodParseResponse;
import com.healthclaw.server.entity.UserProfile;
import java.util.ArrayList;
import java.util.List;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Value("${zhipu.api.key}")
    private String apiKey;

    @Value("${zhipu.api.url}")
    private String apiUrl;

    @Value("${zhipu.model}")
    private String model;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public String chatRaw(String systemPrompt, String userPrompt, float temperature, int maxTokens) throws IOException {
        return chat(systemPrompt, userPrompt, temperature, maxTokens);
    }

    private String chat(String systemPrompt, String userPrompt, float temperature, int maxTokens) throws IOException {
        String body = mapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("model", model);
            put("temperature", temperature);
            put("max_tokens", maxTokens);
            put("messages", java.util.List.of(
                    java.util.Map.of("role", "system", "content", systemPrompt),
                    java.util.Map.of("role", "user", "content", userPrompt)
            ));
        }});

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("error")) {
                throw new IOException("AI API error: " + root.get("error").toString());
            }
            return root.path("choices").get(0).path("message").path("content").asText();
        }
    }

    public List<FoodParseResponse> parseFoodList(String userInput) throws IOException {
        String systemPrompt = """
                你是专业营养分析助手。用户可能描述了一餐或多个餐次的食物，请逐一解析每种食物，返回严格 JSON 数组，不含其他文字。
                每个元素格式：{"food_name":"名称","quantity":"数量","calories_kcal":数值,"calories_min_kcal":最小,"calories_max_kcal":最大,"kj_value":kJ数值,"is_fuzzy":true或false,"confidence":"HIGH或MEDIUM或LOW","protein_g":蛋白质克数,"carbs_g":碳水化合物克数,"fat_g":脂肪克数,"meal_type":"餐次"}
                meal_type 必须根据每种食物所属的餐次单独判断：早上/早餐→BREAKFAST，中午/午餐→LUNCH，下午茶/加餐→SNACK，晚上/晚餐→DINNER，不明确则→BREAKFAST。
                同一段描述中不同餐次的食物必须分别设置各自的 meal_type，不可统一设为同一餐次。
                描述模糊时 is_fuzzy=true。protein_g/carbs_g/fat_g 为该份量估算克数，不可为0，必须给出合理估算值。只返回 JSON 数组，如 [{...},{...}]。""";

        String content = chat(systemPrompt, userInput, 0.2f, 600);
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) throw new IOException("AI 返回格式错误: " + content);

        JsonNode arr = mapper.readTree(content.substring(start, end + 1));
        List<FoodParseResponse> result = new ArrayList<>();
        for (JsonNode obj : arr) {
            FoodParseResponse resp = new FoodParseResponse();
            resp.setFoodName(obj.path("food_name").asText());
            resp.setQuantity(obj.path("quantity").asText());
            resp.setCaloriesKcal(obj.path("calories_kcal").asInt());
            resp.setCaloriesMinKcal(obj.path("calories_min_kcal").asInt(resp.getCaloriesKcal()));
            resp.setCaloriesMaxKcal(obj.path("calories_max_kcal").asInt(resp.getCaloriesKcal()));
            resp.setKjValue((float) obj.path("kj_value").asDouble(resp.getCaloriesKcal() * 4.184));
            resp.setFuzzy(obj.path("is_fuzzy").asBoolean(false));
            resp.setConfidence(obj.path("confidence").asText("MEDIUM"));
            resp.setProtein(obj.path("protein_g").asDouble(0));
            resp.setCarbs(obj.path("carbs_g").asDouble(0));
            resp.setFat(obj.path("fat_g").asDouble(0));
            String mealType = obj.path("meal_type").asText("BREAKFAST");
            resp.setMealType(mealType.isEmpty() ? "BREAKFAST" : mealType);
            result.add(resp);
        }
        return result;
    }

    public List<ExerciseParseResponse> parseExerciseList(String userInput) throws IOException {
        String systemPrompt = """
                你是专业健身教练。用户描述了一项或多项运动，请逐一解析，只返回 JSON 数组，不含任何其他文字、代码块标记或解释。
                每个元素格式：{"exercise_name":"运动名称","duration_minutes":时长分钟数,"calories_burned_kcal":消耗热量整数,"intensity_level":"LOW或MEDIUM或HIGH"}
                根据运动强度和体重70kg估算热量消耗。输出示例：[{"exercise_name":"跑步","duration_minutes":30,"calories_burned_kcal":250,"intensity_level":"MEDIUM"}]""";

        String content = chat(systemPrompt, userInput, 0.2f, 400);
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) throw new IOException("AI 返回格式错误: " + content);

        JsonNode arr = mapper.readTree(content.substring(start, end + 1));
        List<ExerciseParseResponse> result = new ArrayList<>();
        for (JsonNode obj : arr) {
            ExerciseParseResponse resp = new ExerciseParseResponse();
            resp.setExerciseName(obj.path("exercise_name").asText());
            resp.setDurationMinutes(obj.path("duration_minutes").asInt(30));
            resp.setCaloriesBurnedKcal(obj.path("calories_burned_kcal").asInt());
            resp.setIntensityLevel(obj.path("intensity_level").asText("MEDIUM"));
            result.add(resp);
        }
        return result;
    }

    public String generateDailyComment(DailyCommentRequest req) throws IOException {
        String systemPrompt = "你是温暖的减肥健康顾问，用简短鼓励性语言给用户每日反馈。一句话，不超过60字，语气积极，包含数据洞察，不说教。";
        int gap = req.getBmrKcal() + req.getBurnedKcal() - req.getIntakeKcal();
        String gapText = gap >= 0 ? "热量缺口 " + gap + " kcal" : "热量盈余 " + (-gap) + " kcal";
        String userPrompt = String.format("今日：摄入%dkcal，运动消耗%dkcal，基础代谢%dkcal，%s。体重%.1fkg，目标%.1fkg。",
                req.getIntakeKcal(), req.getBurnedKcal(), req.getBmrKcal(), gapText, req.getWeightKg(), req.getTargetWeightKg());
        return chat(systemPrompt, userPrompt, 0.8f, 128).trim();
    }

    public String recommendMeal(UserProfile profile) throws IOException {
        String systemPrompt = "你是专业营养师，根据用户信息推荐今日三餐食谱。简洁实用，每餐一行，标注大致热量，总热量接近目标摄入量。";
        String userPrompt = String.format("用户：%s，%d岁，%.0fcm，%.1fkg，目标%.1fkg。今日推荐摄入%dkcal。请推荐今日三餐。",
                profile.getGender().equals("MALE") ? "男" : "女",
                profile.getAgeYears(), profile.getHeightCm(),
                profile.getCurrentWeightKg(), profile.getTargetWeightKg(),
                profile.getEffectiveDailyGoal());
        return chat(systemPrompt, userPrompt, 0.7f, 300).trim();
    }

    public String recommendExercise(UserProfile profile) throws IOException {
        String systemPrompt = "你是专业健身教练，根据用户信息推荐今日运动计划。简洁实用，列出2-3项运动，标注时长和消耗热量。";
        String userPrompt = String.format("用户：%s，%d岁，%.1fkg，目标%.1fkg。推荐今日运动计划。",
                profile.getGender().equals("MALE") ? "男" : "女",
                profile.getAgeYears(), profile.getCurrentWeightKg(), profile.getTargetWeightKg());
        return chat(systemPrompt, userPrompt, 0.7f, 200).trim();
    }
}
