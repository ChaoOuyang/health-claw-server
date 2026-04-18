package com.healthclaw.server.agent;

import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.repository.ExerciseRecordRepository;
import com.healthclaw.server.repository.FoodRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HermesAgentServiceTest {

    @Autowired
    private HermesAgentService agentService;

    @Autowired
    private FoodRecordRepository foodRepo;

    @Autowired
    private ExerciseRecordRepository exerciseRepo;

    private static final String DATE = "2024-01-15";

    @BeforeEach
    void setUp() throws InterruptedException {
        foodRepo.deleteAll();
        exerciseRepo.deleteAll();
        Thread.sleep(1500);
    }

    @Test
    @DisplayName("同一天多餐追加：早上吃了牛奶和面包，中午吃了米饭和红烧肉")
    void same_day_multi_meal_append() throws Exception {
        agentService.process("我上午吃了牛奶和面包", DATE);
        agentService.process("中午吃了米饭和红烧肉", DATE);

        List<FoodRecord> all = foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE);
        assertThat(all).hasSize(4);

        List<FoodRecord> breakfast = foodRepo.findByRecordDateAndMealTypeOrderByRecordTimestampAsc(DATE, "BREAKFAST");
        assertThat(breakfast).hasSize(2);
        assertThat(breakfast).extracting("foodName").anyMatch(n -> ((String) n).contains("牛奶"));
        assertThat(breakfast).extracting("foodName").anyMatch(n -> ((String) n).contains("面包"));

        List<FoodRecord> lunch = foodRepo.findByRecordDateAndMealTypeOrderByRecordTimestampAsc(DATE, "LUNCH");
        assertThat(lunch).hasSize(2);
        assertThat(lunch).extracting("foodName").anyMatch(n -> ((String) n).contains("米饭"));
        assertThat(lunch).extracting("foodName").anyMatch(n -> ((String) n).contains("红烧肉"));
    }

    @Test
    @DisplayName("追加后删除指定食物：早餐记了苹果和香蕉，删掉苹果，香蕉保留")
    void append_then_delete_specific() throws Exception {
        agentService.process("早上吃了苹果和香蕉", DATE);
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(2);

        agentService.process("删掉苹果", DATE);

        List<FoodRecord> remaining = foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getFoodName()).containsIgnoringCase("香蕉");
    }

    @Test
    @DisplayName("删除后继续追加：删早餐豆浆后，再记午餐炒面，DB 只有午餐")
    void delete_then_append() throws Exception {
        agentService.process("早上喝了豆浆", DATE);
        agentService.process("删掉豆浆", DATE);
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).isEmpty();

        agentService.process("中午吃了炒面", DATE);

        List<FoodRecord> remaining = foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getMealType()).isEqualTo("LUNCH");
    }

    @Test
    @DisplayName("三餐追加后查询：总摄入卡路里正确累加")
    void three_meals_then_query_total() throws Exception {
        agentService.process("早上吃了燕麦粥", DATE);
        agentService.process("中午吃了盖浇饭", DATE);
        agentService.process("晚上吃了水煮鱼", DATE);

        AgentResponse resp = agentService.process("今天吃了多少卡路里", DATE);

        assertThat(resp.getIntent()).isEqualTo("QUERY_TODAY");
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(3);
        int total = foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)
                .stream().mapToInt(FoodRecord::getCaloriesKcal).sum();
        assertThat(total).isBetween(400, 2000);
    }
}
