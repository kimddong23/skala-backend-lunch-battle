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
@Tag(name = "5. 햄스터 미로 경주",
     description = "후보 메뉴가 햄스터로 미로를 달린다. 출구를 먼저 찾은 메뉴가 오늘의 점심")
public class RaceController {

    private final RaceService raceService;

    @PostMapping
    @Operation(summary = "경주 진행",
            description = "출발 직전에 미로와 스탯을 난수로 뽑아 경주한다. 최단 경로는 BFS 로 구하고, "
                        + "햄스터는 판단력만큼의 확률로 그 길을 따른다. 결과에 따라 배틀이 마감된다. "
                        + "배틀당 한 번만 달릴 수 있다")
    public ResponseEntity<RaceDto> run(@PathVariable Long battleId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raceService.run(battleId));
    }

    @GetMapping
    @Operation(summary = "경주 다시 보기",
            description = "저장된 시드로 미로부터 주행까지 같은 경기를 그대로 재현한다")
    public ResponseEntity<RaceDto> replay(@PathVariable Long battleId) {
        return ResponseEntity.ok(raceService.replay(battleId));
    }

    @DeleteMapping
    @Operation(summary = "다시 하기",
            description = "경주 기록을 지우고 배틀을 다시 연다. 후보와 표는 그대로 두므로 "
                        + "같은 참가자로 새 미로에서 다시 달릴 수 있다")
    public ResponseEntity<Void> reset(@PathVariable Long battleId) {
        raceService.reset(battleId);
        return ResponseEntity.noContent().build();
    }
}
