package com.skala.lunch.controller;

import com.skala.lunch.dto.*;
import com.skala.lunch.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 분석 API. 조회는 모두 SQL Mapper(MyBatis) 의 SQL 로 처리한다.
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@Tag(name = "5. 분석 (MyBatis)", description = "조인·집계·순위 — SQL Mapper 로 처리")
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/ranking")
    @Operation(summary = "식당 종합 랭킹",
            description = "우승 횟수·누적 득표·평점·우승률을 한 번의 SQL 로 집계. 후보로 오른 적 있는 식당만")
    public ResponseEntity<List<RankingDto>> ranking() {
        return ResponseEntity.ok(analysisService.ranking());
    }

    @GetMapping("/department-taste")
    @Operation(summary = "부서별 취향",
            description = "어느 부서가 어떤 분류에 표를 몰아주는가 (부서 내 비중 %)")
    public ResponseEntity<List<DepartmentTasteDto>> departmentTaste() {
        return ResponseEntity.ok(analysisService.departmentTaste());
    }

    @GetMapping("/weekday-trend")
    @Operation(summary = "요일별 인기 분류",
            description = "월요일엔 국물, 금요일엔 고기 같은 경향을 본다")
    public ResponseEntity<List<WeekdayTrendDto>> weekdayTrend() {
        return ResponseEntity.ok(analysisService.weekdayTrend());
    }

    @GetMapping("/picky-index")
    @Operation(summary = "개인 편식 지수",
            description = "한 분류에 표를 몰아준 비율과 판정 한마디")
    public ResponseEntity<List<PickyIndexDto>> pickyIndex() {
        return ResponseEntity.ok(analysisService.pickyIndex());
    }

    @GetMapping("/participation")
    @Operation(summary = "배틀별 투표 참여율",
            description = "전체 사원 대비 실제 투표 인원")
    public ResponseEntity<List<ParticipationDto>> participation() {
        return ResponseEntity.ok(analysisService.participation());
    }

    @GetMapping("/cheer-effect")
    @Operation(summary = "응원 무용지수",
            description = "최다 득표 메뉴가 실제로 우승했는지 배틀별로 본다. "
                        + "경주는 득표를 보지 않으므로 맞는 비율은 우연 수준에 머문다")
    public ResponseEntity<List<CheerEffectDto>> cheerEffect() {
        return ResponseEntity.ok(analysisService.cheerEffect());
    }

    @GetMapping("/category-share")
    @Operation(summary = "분류별 득표 점유율",
            description = "회사 전체 입맛 지도")
    public ResponseEntity<List<CategoryShareDto>> categoryShare() {
        return ResponseEntity.ok(analysisService.categoryShare());
    }
}
