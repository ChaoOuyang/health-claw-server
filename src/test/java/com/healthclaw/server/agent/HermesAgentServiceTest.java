package com.healthclaw.server.agent;

import com.healthclaw.server.entity.FoodRecord;
import com.healthclaw.server.repository.ExerciseRecordRepository;
import com.healthclaw.server.repository.FoodRecordRepository;
import com.healthclaw.server.repository.WeightRecordRepository;
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

    @Autowired
    private WeightRecordRepository weightRepo;

    private static final String DATE = "2024-01-15";

    @BeforeEach
    void setUp() throws InterruptedException {
        foodRepo.deleteAll();
        exerciseRepo.deleteAll();
        weightRepo.deleteAll();
        Thread.sleep(1500);
    }

    @Test
    @DisplayName("多餐次：早上吃了牛奶和面包，中午吃了米饭和红烧肉，各自落到正确餐次")
    void multi_meal_correct_meal_type() throws Exception {
        agentService.process("早上吃了牛奶和面包，中午吃了米饭和红烧肉", DATE);

        List<FoodRecord> breakfast = foodRepo.findByRecordDateAndMealTypeOrderByRecordTimestampAsc(DATE, "BREAKFAST");
        List<FoodRecord> lunch = foodRepo.findByRecordDateAndMealTypeOrderByRecordTimestampAsc(DATE, "LUNCH");

        assertThat(breakfast).hasSize(2);
        assertThat(breakfast).extracting("foodName").anyMatch(n -> ((String) n).contains("牛奶"));
        assertThat(breakfast).extracting("foodName").anyMatch(n -> ((String) n).contains("面包"));

        assertThat(lunch).hasSize(2);
        assertThat(lunch).extracting("foodName").anyMatch(n -> ((String) n).contains("米饭"));
        assertThat(lunch).extracting("foodName").anyMatch(n -> ((String) n).contains("红烧肉"));
    }

    @Test
    @DisplayName("饮食+运动混合：中午吃了炒饭，跑步30分钟，两条数据各自落到正确板块")
    void food_and_exercise_correct_storage() throws Exception {
        agentService.process("中午吃了炒饭，跑步30分钟", DATE);

        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(1);
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE).get(0).getMealType()).isEqualTo("LUNCH");

        assertThat(exerciseRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(1);
        assertThat(exerciseRepo.findByRecordDateOrderByRecordTimestampAsc(DATE).get(0).getDurationMinutes()).isBetween(25, 35);
    }

    @Test
    @DisplayName("饮食+体重混合：早上吃了鸡蛋，体重65kg，两条数据各自落到正确板块")
    void food_and_weight_correct_storage() throws Exception {
        agentService.process("早上吃了鸡蛋，体重65kg", DATE);

        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(1);
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE).get(0).getMealType()).isEqualTo("BREAKFAST");

        assertThat(weightRepo.findByRecordDate(DATE)).isPresent();
        assertThat(weightRepo.findByRecordDate(DATE).get().getWeightKg()).isBetween(64f, 66f);
    }

    @Test
    @DisplayName("饮食+运动+体重全混合：今天早餐吃了燕麦，跑步20分钟，体重70kg，三条数据全部入库")
    void food_exercise_weight_all_correct() throws Exception {
        agentService.process("今天早餐吃了燕麦，跑步20分钟，体重70kg", DATE);

        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(1);
        assertThat(foodRepo.findByRecordDateOrderByRecordTimestampAsc(DATE).get(0).getMealType()).isEqualTo("BREAKFAST");

        assertThat(exerciseRepo.findByRecordDateOrderByRecordTimestampAsc(DATE)).hasSize(1);

        assertThat(weightRepo.findByRecordDate(DATE)).isPresent();
        assertThat(weightRepo.findByRecordDate(DATE).get().getWeightKg()).isBetween(69f, 71f);
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
}
