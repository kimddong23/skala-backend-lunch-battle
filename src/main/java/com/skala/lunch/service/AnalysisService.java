package com.skala.lunch.service;

import com.skala.lunch.dto.*;
import com.skala.lunch.mapper.LunchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 분석 기능. 조회는 모두 SQL Mapper 로 처리한다.
 *
 * 여러 테이블을 조인해 합계·그룹핑·순위를 매기는 일이라
 * 자바에서 반복문으로 계산하는 것보다 SQL 한 문장이 짧고 왕복도 한 번이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final LunchMapper lunchMapper;

    /** 분석 1 — 식당 종합 랭킹. */
    public List<RankingDto> ranking() {
        List<RankingDto> list = lunchMapper.findRanking();
        AtomicInteger rank = new AtomicInteger(1);
        list.forEach(r -> r.setRank(rank.getAndIncrement()));
        return list;
    }

    /** 분석 2 — 부서별 취향. */
    public List<DepartmentTasteDto> departmentTaste() {
        return lunchMapper.findDepartmentTaste();
    }

    /** 분석 3 — 요일별 인기 분류. */
    public List<WeekdayTrendDto> weekdayTrend() {
        return lunchMapper.findWeekdayTrend();
    }

    /** 분석 4 — 개인 편식 지수. 판정 문구는 조회 후 붙인다. */
    public List<PickyIndexDto> pickyIndex() {
        List<PickyIndexDto> list = lunchMapper.findPickyIndex();
        list.forEach(p -> p.setVerdict(
                BattleComments.pickyVerdict(p.getPickyPercent() / 100.0, p.getTopCategory())));
        return list;
    }

    /** 분석 5 — 배틀별 투표 참여율. */
    public List<ParticipationDto> participation() {
        return lunchMapper.findParticipation();
    }

    /** 분석 6 — 분류별 득표 점유율. */
    public List<CategoryShareDto> categoryShare() {
        return lunchMapper.findCategoryShare();
    }
}
