package com.skala.lunch.controller;

import com.skala.lunch.dto.ReviewDto;
import com.skala.lunch.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Tag(name = "3. 리뷰", description = "식당 평점. 1인 1식당 1리뷰")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "리뷰 작성 또는 수정",
            description = "이미 남긴 리뷰가 있으면 새로 만들지 않고 고친다 (평점 왜곡 방지)")
    public ResponseEntity<ReviewDto> write(@Valid @RequestBody ReviewDto dto) {
        return ResponseEntity.ok(reviewService.write(dto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "리뷰 단건 조회")
    public ResponseEntity<ReviewDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.get(id));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "식당별 리뷰 조회")
    public ResponseEntity<List<ReviewDto>> byRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(reviewService.getByRestaurant(restaurantId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "리뷰 삭제")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
