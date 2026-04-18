package com.healthclaw.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthclaw.server.dto.DailyCommentRequest;
import com.healthclaw.server.dto.ExerciseParseResponse;
import com.healthclaw.server.dto.FoodParseResponse;
import com.healthclaw.server.entity.ExerciseRecord;
import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.entity.UserProfile;
import com.healthclaw.server.entity.WeightRecord;
import com.healthclaw.server.service.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HermesAgentService {

    private final AiService aiService;
    private final FoodRecordService foodService;
    private final ExerciseRecordService exerciseService;
    private final WeightRecordService weightService;
    private final UserProfileService profileService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String INTENT_PROMPT = """
            你是健康记录助手，分析用户输入的意图，返回严格 JSON，不含其他文字。

            意图类型（intent 字段）：
            - FOOD_RECORD：记录饮食
            - EXERCISE_RECORD：记录运动
            - FOOD_AND_EXERCISE：同时包含饮食和运动
            - WEIGHT_RECORD：记录体重
            - QUERY_TODAY：查询今日数据
            - DAILY_COMMENT：请求 AI 点评/总结
            - UNKNOWN：无法识别

            返回格式：
            {
              "intent": "...",
              "food_text": "饮食相关描述，无则null",
              "exercise_text": "运动相关描述，无则null",
              "meal_type": "BREAKFAST|LUNCH|DINNER|SNACK，根据时间词推断，不明确则null",
              "weight_kg": 体重数值或null
            }

            只返回 JSON，不含任何其他内容。
            """;

    public HermesAgentService(AiService aiService, FoodRecordService foodService,
                               ExerciseRecordService exerciseService, WeightRecordService weightService,
                               UserProfileService profileService) {
        this.aiService = aiService;
        this.foodService = foodService;
        this.exerciseService = exerciseService;
        this.weightService = weightService;
        this.profileService = profileService;
    }

    public AgentResponse process(String message, String date) {
        if (date == null || date.isBlank()) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        try {
            IntentResult intent = parseIntent(message);
            intent.setRawMessage(message);
            return route(intent, date);
        } catch (Exception e) {
            return AgentResponse.of("UNKNOWN", "抱歉，我没理解你说的，可以换个方式再说一次吗？");
        }
    }

    private IntentResult parseIntent(String message) throws Exception {
        String raw = aiService.chatRaw(INTENT_PROMPT, message, 0.1f, 200);
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) throw new Exception("意图解析格式错误");

        JsonNode node = mapper.readTree(raw.substring(start, end + 1));
        IntentResult result = new IntentResult();
        result.setIntent(node.path("intent").asText("UNKNOWN"));
        result.setFoodText(nullIfEmpty(node.path("food_text").asText(null)));
        result.setExerciseText(nullIfEmpty(node.path("exercise_text").asText(null)));
        result.setMealType(nullIfEmpty(node.path("meal_type").asText(null)));
        if (!node.path("weight_kg").isMissingNode() && !node.path("weight_kg").isNull()) {
            result.setWeightKg(node.path("weight_kg").asDouble());
        }
        return result;
    }

    private AgentResponse route(IntentResult intent, String date) throws Exception {
        return switch (intent.getIntent()) {
            case "FOOD_RECORD" -> handleFood(intent, date);
            case "EXERCISE_RECORD" -> handleExercise(intent, date);
            case "FOOD_AND_EXERCISE" -> handleFoodAndExercise(intent, date);
            case "WEIGHT_RECORD" -> handleWeight(intent, date);
            case "QUERY_TODAY" -> handleQueryToday(date);
            case "DAILY_COMMENT" -> handleDailyComment(date);
            default -> AgentResponse.of("UNKNOWN", "没听清楚，可以说'记录午饭吃了什么'或者'今天跑了多久'？");
        };
    }

    private AgentResponse handleFood(IntentResult intent, String date) throws Exception {
        String text = intent.getFoodText() != null ? intent.getFoodText() : intent.getRawMessage();
        List<FoodParseResponse> foods = aiService.parseFoodList(text);
        if (foods.isEmpty()) return AgentResponse.of("FOOD_RECORD", "没解析出食物，可以说得更具体吗？");

        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (FoodParseResponse f : foods) {
            FoodRecord record = new FoodRecord();
            record.setRecordDate(date);
            record.setMealType(resolveMealType(f.getMealType(), intent.getMealType()));
            record.setRawInputText(text);
            record.setFoodName(f.getFoodName());
            record.setQuantity(f.getQuantity());
            record.setCaloriesKcal(f.getCaloriesKcal());
            record.setCaloriesMinKcal(f.getCaloriesMinKcal());
            record.setCaloriesMaxKcal(f.getCaloriesMaxKcal());
            record.setFuzzy(f.isFuzzy());
            record.setKjValue(f.getKjValue());
            foodService.save(record);
            total += f.getCaloriesKcal();
            sb.append(f.getFoodName()).append("(").append(f.getCaloriesKcal()).append("kcal) ");
        }
        return AgentResponse.of("FOOD_RECORD",
                "已记录：" + sb.toString().trim() + "，合计 " + total + " kcal ✓");
    }

    private AgentResponse handleExercise(IntentResult intent, String date) throws Exception {
        String text = intent.getExerciseText() != null ? intent.getExerciseText() : intent.getRawMessage();
        List<ExerciseParseResponse> exercises = aiService.parseExerciseList(text);
        if (exercises.isEmpty()) return AgentResponse.of("EXERCISE_RECORD", "没解析出运动，可以说得更具体吗？");

        int total = 0;
        StringBuilder sb = new StringBuilder();
        for (ExerciseParseResponse e : exercises) {
            ExerciseRecord record = new ExerciseRecord();
            record.setRecordDate(date);
            record.setExerciseName(e.getExerciseName());
            record.setExerciseType(e.getExerciseName());
            record.setDurationMinutes(e.getDurationMinutes());
            record.setCaloriesBurnedKcal(e.getCaloriesBurnedKcal());
            record.setIntensityLevel(e.getIntensityLevel());
            exerciseService.save(record);
            total += e.getCaloriesBurnedKcal();
            sb.append(e.getExerciseName()).append(" ").append(e.getDurationMinutes()).append("min ");
        }
        return AgentResponse.of("EXERCISE_RECORD",
                "已记录：" + sb.toString().trim() + "，消耗 " + total + " kcal ✓");
    }

    private AgentResponse handleFoodAndExercise(IntentResult intent, String date) throws Exception {
        AgentResponse foodResp = handleFood(intent, date);
        AgentResponse exResp = handleExercise(intent, date);
        return AgentResponse.of("FOOD_AND_EXERCISE", foodResp.getReply() + "\n" + exResp.getReply());
    }

    private AgentResponse handleWeight(IntentResult intent, String date) {
        if (intent.getWeightKg() == null) {
            return AgentResponse.of("WEIGHT_RECORD", "没听到体重数值，可以说'今天体重 65.5 公斤'？");
        }
        WeightRecord record = new WeightRecord();
        record.setRecordDate(date);
        record.setWeightKg((float) intent.getWeightKg().doubleValue());
        weightService.upsert(record);
        return AgentResponse.of("WEIGHT_RECORD",
                "已记录今日体重 " + intent.getWeightKg() + " kg ✓");
    }

    private AgentResponse handleQueryToday(String date) {
        int intake = foodService.getTotalCalories(date);
        int burned = exerciseService.getTotalBurned(date);
        UserProfile profile = profileService.getProfile();
        int bmr = profile.getEffectiveBmr();
        int goal = profile.getEffectiveDailyGoal();
        int gap = bmr + burned - intake;
        String gapText = gap >= 0 ? "热量缺口 " + gap + " kcal" : "热量盈余 " + (-gap) + " kcal";
        return AgentResponse.of("QUERY_TODAY",
                "今日摄入 " + intake + " kcal，运动消耗 " + burned + " kcal，目标 " + goal + " kcal，" + gapText);
    }

    private AgentResponse handleDailyComment(String date) throws Exception {
        int intake = foodService.getTotalCalories(date);
        int burned = exerciseService.getTotalBurned(date);
        UserProfile profile = profileService.getProfile();
        DailyCommentRequest req = new DailyCommentRequest();
        req.setIntakeKcal(intake);
        req.setBurnedKcal(burned);
        req.setBmrKcal(profile.getEffectiveBmr());
        req.setWeightKg(profile.getCurrentWeightKg());
        req.setTargetWeightKg(profile.getTargetWeightKg());
        String comment = aiService.generateDailyComment(req);
        return AgentResponse.of("DAILY_COMMENT", comment);
    }

    private String resolveMealType(String fromAi, String fromIntent) {
        if (fromIntent != null && !fromIntent.isBlank()) return fromIntent;
        if (fromAi != null && !fromAi.isBlank()) return fromAi;
        return "BREAKFAST";
    }

    private String nullIfEmpty(String s) {
        if (s == null || s.isBlank() || s.equals("null")) return null;
        return s;
    }
}
