package com.skala.lunch.battle;

import com.skala.lunch.battle.BattleDto;
import com.skala.lunch.battle.CandidateDto;
import com.skala.lunch.battle.VoteRequestDto;
import com.skala.lunch.battle.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/battles")
@RequiredArgsConstructor
@Tag(name = "4. 점심 배틀", description = "후보 등록 · 투표 · 마감")
public class BattleController {

    private final BattleService battleService;

    @PostMapping
    @Operation(summary = "배틀 열기", description = "하루에 하나만. 마감 시각 미지정 시 11:30")
    public ResponseEntity<BattleDto> open(@Valid @RequestBody BattleDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(battleService.openBattle(dto));
    }

    @GetMapping
    @Operation(summary = "전체 배틀 조회")
    public ResponseEntity<List<BattleDto>> getAll() {
        return ResponseEntity.ok(battleService.getAllBattles());
    }

    @GetMapping("/{id}")
    @Operation(summary = "배틀 현황 조회", description = "후보별 득표·득표율 포함")
    public ResponseEntity<BattleDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(battleService.getBattle(id));
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "날짜로 배틀 조회")
    public ResponseEntity<BattleDto> byDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(battleService.getBattleByDate(date));
    }

    @PostMapping("/{id}/candidates")
    @Operation(summary = "후보 등록", description = "같은 식당을 두 번 올릴 수 없음 (409)")
    public ResponseEntity<CandidateDto> addCandidate(@PathVariable Long id,
                                                     @RequestParam Long restaurantId,
                                                     @RequestParam Long memberId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(battleService.addCandidate(id, restaurantId, memberId));
    }

    @DeleteMapping("/{id}/candidates/{candidateId}")
    @Operation(summary = "후보 내리기", description = "이미 표를 받았으면 409")
    public ResponseEntity<Void> removeCandidate(@PathVariable Long id, @PathVariable Long candidateId) {
        battleService.removeCandidate(id, candidateId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/votes")
    @Operation(summary = "투표", description = "1인 1표. 이미 투표했으면 409")
    public ResponseEntity<BattleDto> vote(@PathVariable Long id,
                                          @Valid @RequestBody VoteRequestDto request) {
        return ResponseEntity.ok(battleService.vote(id, request));
    }

    @DeleteMapping("/{id}/votes")
    @Operation(summary = "투표 취소", description = "마감 전까지만 가능")
    public ResponseEntity<BattleDto> cancelVote(@PathVariable Long id, @RequestParam Long memberId) {
        return ResponseEntity.ok(battleService.cancelVote(id, memberId));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "마감하고 우승 확정",
            description = "득표가 가장 많은 후보가 우승. 동점이면 평점 높은 쪽, 그다음 가까운 쪽")
    public ResponseEntity<BattleDto> close(@PathVariable Long id) {
        return ResponseEntity.ok(battleService.closeBattle(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "배틀 삭제", description = "후보·투표가 있으면 409")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        battleService.deleteBattle(id);
        return ResponseEntity.noContent().build();
    }
}
