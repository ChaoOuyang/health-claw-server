package com.healthclaw.server.service;

import com.healthclaw.server.entity.WeightRecord;
import com.healthclaw.server.repository.WeightRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class WeightRecordService {

    private final WeightRecordRepository repo;

    public WeightRecordService(WeightRecordRepository repo) {
        this.repo = repo;
    }

    public List<WeightRecord> getHistory(int days) {
        String endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String startDate = LocalDate.now().minusDays(days - 1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        return repo.findByRecordDateBetweenOrderByRecordDateAsc(startDate, endDate);
    }

    public WeightRecord upsert(WeightRecord record) {
        WeightRecord existing = repo.findByRecordDate(record.getRecordDate()).orElse(null);
        if (existing != null) {
            existing.setWeightKg(record.getWeightKg());
            existing.setNote(record.getNote() != null ? record.getNote() : "");
            existing.setRecordTimestamp(System.currentTimeMillis());
            return repo.save(existing);
        }
        record.setRecordTimestamp(System.currentTimeMillis());
        return repo.save(record);
    }

    public List<WeightRecord> getRecentForPrediction() {
        List<WeightRecord> desc = repo.findTop14ByOrderByRecordDateDesc();
        java.util.Collections.reverse(desc); // 改为升序返回
        return desc;
    }
}
