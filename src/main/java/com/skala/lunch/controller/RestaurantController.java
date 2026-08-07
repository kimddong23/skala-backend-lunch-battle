package com.skala.lunch.controller;

import com.skala.lunch.dto.RestaurantDto;
import com.skala.lunch.entity.Restaurant;
import com.skala.lunch.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Tag(name = "2. 식당", description = "후보로 올릴 식당 관리")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping
    @Operation(summary = "식당 등록")
    public ResponseEntity<RestaurantDto> create(@Valid @RequestBody RestaurantDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantService.create(dto));
    }

    @GetMapping
    @Operation(summary = "전체 식당 조회", description = "평균 평점·리뷰 수·우승 횟수 포함")
    public ResponseEntity<List<RestaurantDto>> getAll() {
        return ResponseEntity.ok(restaurantService.getAll());
    }

    @GetMapping("/active")
    @Operation(summary = "영업 중인 식당만 조회")
    public ResponseEntity<List<RestaurantDto>> getActive() {
        return ResponseEntity.ok(restaurantService.getActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "식당 단건 조회")
    public ResponseEntity<RestaurantDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.get(id));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "분류별 조회")
    public ResponseEntity<List<RestaurantDto>> byCategory(@PathVariable Restaurant.Category category) {
        return ResponseEntity.ok(restaurantService.getByCategory(category));
    }

    @GetMapping("/within/{minutes}")
    @Operation(summary = "도보 N분 이내 조회", description = "비 오는 날이나 회의가 빠듯할 때")
    public ResponseEntity<List<RestaurantDto>> within(@PathVariable Integer minutes) {
        return ResponseEntity.ok(restaurantService.getWithinWalk(minutes));
    }

    @PutMapping("/{id}")
    @Operation(summary = "식당 수정")
    public ResponseEntity<RestaurantDto> update(@PathVariable Long id, @Valid @RequestBody RestaurantDto dto) {
        return ResponseEntity.ok(restaurantService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "식당 삭제", description = "후보·우승·리뷰 기록이 있으면 409")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        restaurantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
