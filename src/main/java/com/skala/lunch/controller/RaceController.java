package com.skala.lunch.controller;

import com.skala.lunch.dto.RaceDto;
import com.skala.lunch.service.RaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/battles/{battleId}/race")
@RequiredArgsConstructor
@Tag(name = "4. 햄스터 레이스", description = "후보 메뉴가 햄스터로 달린다. 먼저 완주한 메뉴가 오늘의 점심")
public class RaceController {

    private final RaceService raceService;

    @PostMapping
    @Operation(summary = "레이스 진행",
            description = "출발 직전에 스탯을 난수로 뽑아 경주한다. 결과에 따라 배틀이 마감된다. "
                        + "배틀당 한 번만 달릴 수 있다")
    public ResponseEntity<RaceDto> run(@PathVariable Long battleId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raceService.run(battleId));
    }

    @GetMapping
    @Operation(summary = "레이스 다시 보기",
            description = "저장된 시드로 같은 경기를 그대로 재현한다")
    public ResponseEntity<RaceDto> replay(@PathVariable Long battleId) {
        return ResponseEntity.ok(raceService.replay(battleId));
    }
}
