package com.healthclaw.server.repository;

import com.healthclaw.server.entity.ProductNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductNoteRepository extends JpaRepository<ProductNote, Long> {
    List<ProductNote> findAllByOrderByCreatedAtDesc();
}
