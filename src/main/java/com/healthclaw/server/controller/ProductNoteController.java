package com.healthclaw.server.controller;

import com.healthclaw.server.dto.ApiResponse;
import com.healthclaw.server.entity.ProductNote;
import com.healthclaw.server.repository.ProductNoteRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
public class ProductNoteController {

    private final ProductNoteRepository repo;

    public ProductNoteController(ProductNoteRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ApiResponse<List<ProductNote>> list() {
        return ApiResponse.ok(repo.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping
    public ApiResponse<ProductNote> create(@RequestBody Map<String, String> body) {
        ProductNote note = new ProductNote();
        note.setContent(body.getOrDefault("content", "").trim());
        note.setCreatedAt(System.currentTimeMillis());
        return ApiResponse.ok(repo.save(note));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ApiResponse.ok(null);
    }
}
