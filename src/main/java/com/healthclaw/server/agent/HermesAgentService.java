package com.healthclaw.server.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HermesAgentService {

    private static final Logger log = LoggerFactory.getLogger(HermesAgentService.class);

    private final AiService aiService;
    private final FoodRecordService foodService;
    private final ExerciseRecordService exerciseService;
    private final WeightRecordService weightService;
    private final UserProfileService profileService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String INTENT_PROMPT = """
            你是健康记录助手，分析用户输入包含哪些类型的信息，返回严格 JSON，不含其他文字。

            tags 数组取值（可同时包含多个）：
            - FOOD：包含饮食记录（可跨多个餐次）
            - EXERCISE：包含运动记录
            - WEIGHT：包含体重记录
            - DELETE_FOOD：删除某条饮食记录（含"删掉""去掉""取消""撤销"等词，且指向食物）
            - DELETE_EXERCISE：删除某条运动记录（含"删掉""去掉""取消""撤销"等词，且指向运动）
            - QUERY_TODAY：查询今日数据
            - DAILY_COMMENT：请求 AI 点评/总结
            - UNKNOWN：完全无法识别时单独使用

            返回格式：
            {
              "tags": ["TAG1", "TAG2"],
              "food_text": "完整的饮食描述原文（保留所有餐次和食物信息），无则null",
              "exercise_text": "运动相关描述，无则null",
              "meal_type": "仅当所有食物属于同一餐次时填写 BREAKFAST|LUNCH|DINNER|SNACK，涉及多个餐次则填null",
              "weight_kg": 体重数值或null,
              "delete_keyword": "删除时提取的关键词，如'牛肉面'或'跑步'，无则null"
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
            log.error("Agent process failed for message='{}': {}", message, e.getMessage(), e);
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
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = node.path("tags");
        if (tagsNode.isArray()) {
            tagsNode.forEach(t -> tags.add(t.asText()));
        }
        result.setTags(tags.isEmpty() ? List.of("UNKNOWN") : tags);
        result.setFoodText(nullIfEmpty(node.path("food_text").asText(null)));
        result.setExerciseText(nullIfEmpty(node.path("exercise_text").asText(null)));
        result.setMealType(nullIfEmpty(node.path("meal_type").asText(null)));
        result.setDeleteKeyword(nullIfEmpty(node.path("delete_keyword").asText(null)));
        if (!node.path("weight_kg").isMissingNode() && !node.path("weight_kg").isNull()) {
            result.setWeightKg(node.path("weight_kg").asDouble());
        }
        return result;
    }

    private AgentResponse route(IntentResult intent, String date) throws Exception {
        List<String> tags = intent.getTags();

        if (tags.contains("UNKNOWN") || tags.isEmpty()) {
            return AgentResponse.of("UNKNOWN", "没听清楚，可以说'记录午饭吃了什么'或者'今天跑了多久'？");
        }
        if (tags.contains("QUERY_TODAY"))     return handleQueryToday(date);
        if (tags.contains("DAILY_COMMENT"))   return handleDailyComment(date);
        if (tags.contains("DELETE_FOOD"))     return handleDeleteFood(intent, date);
        if (tags.contains("DELETE_EXERCISE")) return handleDeleteExercise(intent, date);

        List<AgentResponse> responses = new ArrayList<>();
        if (tags.contains("FOOD"))     responses.add(handleFood(intent, date));
        if (tags.contains("EXERCISE")) responses.add(handleExercise(intent, date));
        if (tags.contains("WEIGHT"))   responses.add(handleWeight(intent, date));

        if (responses.isEmpty()) {
            return AgentResponse.of("UNKNOWN", "没听清楚，可以说'记录午饭吃了什么'或者'今天跑了多久'？");
        }
        String combinedTag = tags.stream()
                .filter(t -> List.of("FOOD", "EXERCISE", "WEIGHT").contains(t))
                .collect(Collectors.joining("_AND_"));
        String reply = responses.stream().map(AgentResponse::getReply).collect(Collectors.joining("\n"));
        String reasoning = responses.stream().map(AgentResponse::getReasoning).collect(Collectors.joining("\n"));
        return AgentResponse.of(combinedTag, reply, reasoning);
    }

    private AgentResponse handleFood(IntentResult intent, String date) throws Exception {
        String text = intent.getFoodText() != null ? intent.getFoodText() : intent.getRawMessage();
        List<FoodParseResponse> foods = aiService.parseFoodList(text);
        if (foods.isEmpty()) return AgentResponse.of("FOOD_RECORD", "没解析出食物，可以说得更具体吗？", "识别到饮食意图，但解析结果为空");

        int total = 0;
        StringBuilder sb = new StringBuilder();
        StringBuilder reasoning = new StringBuilder("识别到饮食记录意图，解析出 " + foods.size() + " 项：");
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
            record.setProtein(f.getProtein());
            record.setCarbs(f.getCarbs());
            record.setFat(f.getFat());
            foodService.save(record);
            total += f.getCaloriesKcal();
            sb.append(f.getFoodName()).append("(").append(f.getCaloriesKcal()).append("kcal) ");
            reasoning.append(f.getFoodName()).append(" ≈ ").append(f.getCaloriesKcal()).append("kcal；");
        }
        return AgentResponse.of("FOOD_RECORD",
                "已记录：" + sb.toString().trim() + "，合计 " + total + " kcal ✓",
                reasoning.toString());
    }

    private AgentResponse handleExercise(IntentResult intent, String date) throws Exception {
        String text = intent.getExerciseText() != null ? intent.getExerciseText() : intent.getRawMessage();
        List<ExerciseParseResponse> exercises = aiService.parseExerciseList(text);
        if (exercises.isEmpty()) return AgentResponse.of("EXERCISE_RECORD", "没解析出运动，可以说得更具体吗？", "识别到运动意图，但解析结果为空");

        int total = 0;
        StringBuilder sb = new StringBuilder();
        StringBuilder reasoning = new StringBuilder("识别到运动记录意图，解析出 " + exercises.size() + " 项：");
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
            reasoning.append(e.getExerciseName()).append(" ").append(e.getDurationMinutes()).append("min ≈ ").append(e.getCaloriesBurnedKcal()).append("kcal；");
        }
        return AgentResponse.of("EXERCISE_RECORD",
                "已记录：" + sb.toString().trim() + "，消耗 " + total + " kcal ✓",
                reasoning.toString());
    }

    private AgentResponse handleWeight(IntentResult intent, String date) {
        if (intent.getWeightKg() == null) {
            return AgentResponse.of("WEIGHT_RECORD", "没听到体重数值，可以说'今天体重 65.5 公斤'？", "识别到体重记录意图，但未提取到数值");
        }
        WeightRecord record = new WeightRecord();
        record.setRecordDate(date);
        record.setWeightKg((float) intent.getWeightKg().doubleValue());
        weightService.upsert(record);
        return AgentResponse.of("WEIGHT_RECORD",
                "已记录今日体重 " + intent.getWeightKg() + " kg ✓",
                "识别到体重记录意图，提取数值 " + intent.getWeightKg() + " kg，写入 " + date);
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
                "今日摄入 " + intake + " kcal，运动消耗 " + burned + " kcal，目标 " + goal + " kcal，" + gapText,
                "查询今日数据：摄入=" + intake + " 消耗=" + burned + " BMR=" + bmr);
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
        return AgentResponse.of("DAILY_COMMENT", comment,
                "生成每日点评，摄入=" + intake + " 消耗=" + burned);
    }

    private AgentResponse handleDeleteFood(IntentResult intent, String date) throws Exception {
        List<FoodRecord> foods = foodService.getByDate(date);
        if (foods.isEmpty()) {
            return AgentResponse.of("DELETE_FOOD", "今日暂无饮食记录", "今日无饮食记录可删除");
        }
        // 构建记录列表传给 LLM 决策
        StringBuilder listDesc = new StringBuilder();
        for (FoodRecord f : foods) {
            listDesc.append("id=").append(f.getId()).append(" 名称=").append(f.getFoodName())
                    .append(" 餐次=").append(f.getMealType()).append("；");
        }
        String systemPrompt = "你是健康记录助手。根据用户说的话，从以下今日饮食记录中选出要删除的条目，返回严格 JSON：{\"ids\":[id1,id2,...],\"reason\":\"简短说明\"}，只返回 JSON。\n今日记录：" + listDesc;
        String raw = aiService.chatRaw(systemPrompt, intent.getRawMessage(), 0.1f, 200);
        int start = raw.indexOf('{'); int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return AgentResponse.of("DELETE_FOOD", "AI 决策失败，请重试", "LLM 返回格式异常：" + raw);

        JsonNode node = mapper.readTree(raw.substring(start, end + 1));
        String reason = node.path("reason").asText("AI 决策删除");
        JsonNode idsNode = node.path("ids");
        if (!idsNode.isArray() || idsNode.isEmpty()) {
            return AgentResponse.of("DELETE_FOOD", "没找到匹配的饮食记录", "AI 决策：" + reason);
        }
        StringBuilder deleted = new StringBuilder();
        for (JsonNode idNode : idsNode) {
            long id = idNode.asLong();
            foods.stream().filter(f -> f.getId() == id).findFirst().ifPresent(f -> {
                foodService.delete(f.getId());
                deleted.append(f.getFoodName()).append(" ");
            });
        }
        return AgentResponse.of("DELETE_FOOD",
                "已删除：" + deleted.toString().trim() + " ✓",
                "AI 决策：" + reason);
    }

    private AgentResponse handleDeleteExercise(IntentResult intent, String date) throws Exception {
        List<ExerciseRecord> exercises = exerciseService.getByDate(date);
        if (exercises.isEmpty()) {
            return AgentResponse.of("DELETE_EXERCISE", "今日暂无运动记录", "今日无运动记录可删除");
        }
        StringBuilder listDesc = new StringBuilder();
        for (ExerciseRecord e : exercises) {
            listDesc.append("id=").append(e.getId()).append(" 名称=").append(e.getExerciseName())
                    .append(" 时长=").append(e.getDurationMinutes()).append("min；");
        }
        String systemPrompt = "你是健康记录助手。根据用户说的话，从以下今日运动记录中选出要删除的条目，返回严格 JSON：{\"ids\":[id1,id2,...],\"reason\":\"简短说明\"}，只返回 JSON。\n今日记录：" + listDesc;
        String raw = aiService.chatRaw(systemPrompt, intent.getRawMessage(), 0.1f, 200);
        int start = raw.indexOf('{'); int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return AgentResponse.of("DELETE_EXERCISE", "AI 决策失败，请重试", "LLM 返回格式异常：" + raw);

        JsonNode node = mapper.readTree(raw.substring(start, end + 1));
        String reason = node.path("reason").asText("AI 决策删除");
        JsonNode idsNode = node.path("ids");
        if (!idsNode.isArray() || idsNode.isEmpty()) {
            return AgentResponse.of("DELETE_EXERCISE", "没找到匹配的运动记录", "AI 决策：" + reason);
        }
        StringBuilder deleted = new StringBuilder();
        for (JsonNode idNode : idsNode) {
            long id = idNode.asLong();
            exercises.stream().filter(e -> e.getId() == id).findFirst().ifPresent(e -> {
                exerciseService.delete(e.getId());
                deleted.append(e.getExerciseName()).append(" ");
            });
        }
        return AgentResponse.of("DELETE_EXERCISE",
                "已删除：" + deleted.toString().trim() + " ✓",
                "AI 决策：" + reason);
    }

    private String resolveMealType(String fromAi, String fromIntent) {
        if (fromAi != null && !fromAi.isBlank()) return fromAi;
        if (fromIntent != null && !fromIntent.isBlank()) return fromIntent;
        return "BREAKFAST";
    }

    private String nullIfEmpty(String s) {
        if (s == null || s.isBlank() || s.equals("null")) return null;
        return s;
    }
}
