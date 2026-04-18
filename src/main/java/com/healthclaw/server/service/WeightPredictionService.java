package com.healthclaw.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthclaw.server.dto.WeightPredictResponse;
import com.healthclaw.server.entity.WeightRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class WeightPredictionService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AiService aiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public WeightPredictionService(AiService aiService) {
        this.aiService = aiService;
    }

    public WeightPredictResponse predict(List<WeightRecord> history, int days) {
        WeightPredictResponse resp = new WeightPredictResponse();

        List<WeightPredictResponse.WeightPoint> actual = new ArrayList<>();
        for (WeightRecord r : history) {
            actual.add(new WeightPredictResponse.WeightPoint(r.getRecordDate(), r.getWeightKg()));
        }
        resp.setActual(actual);

        if (history.size() < 2) {
            resp.setPredicted(Collections.emptyList());
            return resp;
        }

        try {
            resp.setPredicted(predictWithLlm(history, days));
        } catch (Exception e) {
            resp.setPredicted(linearFallback(history, days));
        }
        return resp;
    }

    private List<WeightPredictResponse.WeightPoint> predictWithLlm(List<WeightRecord> history, int days) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是用户最近的体重记录（日期升序）：\n");
        for (WeightRecord r : history) {
            sb.append(r.getRecordDate()).append(": ").append(r.getWeightKg()).append("kg\n");
        }
        LocalDate today = LocalDate.now();
        sb.append("\n请根据上述趋势，预测从明天（").append(today.plusDays(1).format(FMT))
          .append("）起未来").append(days).append("天的体重。");
        sb.append("\n只返回 JSON 数组，格式：[{\"date\":\"YYYY-MM-DD\",\"weightKg\":数值}, ...]，不含其他文字。");

        String systemPrompt = "你是体重趋势分析助手。根据历史体重数据，结合实际减重规律（每天减重幅度通常在0~0.3kg），预测未来走势。只返回 JSON 数组。";
        String content = aiService.chatRaw(systemPrompt, sb.toString(), 0.3f, 512);

        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end <= start) throw new Exception("LLM 返回格式错误: " + content);

        JsonNode arr = mapper.readTree(content.substring(start, end + 1));
        List<WeightPredictResponse.WeightPoint> result = new ArrayList<>();
        for (JsonNode node : arr) {
            String date = node.path("date").asText();
            float weight = (float) node.path("weightKg").asDouble();
            if (!date.isEmpty() && weight > 0) {
                result.add(new WeightPredictResponse.WeightPoint(date, weight));
            }
        }
        return result;
    }

    // 线性回归兜底，LLM 失败时使用
    private List<WeightPredictResponse.WeightPoint> linearFallback(List<WeightRecord> history, int days) {
        int n = history.size();
        double[] x = new double[n];
        double[] y = new double[n];
        LocalDate base = LocalDate.parse(history.get(0).getRecordDate(), FMT);
        for (int i = 0; i < n; i++) {
            x[i] = base.until(LocalDate.parse(history.get(i).getRecordDate(), FMT)).getDays();
            y[i] = history.get(i).getWeightKg();
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            sumX += x[i]; sumY += y[i];
            sumXY += x[i] * y[i]; sumX2 += x[i] * x[i];
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        LocalDate today = LocalDate.now();
        double todayX = base.until(today).getDays();
        List<WeightPredictResponse.WeightPoint> predicted = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            float w = (float) Math.max(30, slope * (todayX + i) + intercept);
            predicted.add(new WeightPredictResponse.WeightPoint(today.plusDays(i).format(FMT), w));
        }
        return predicted;
    }
}
