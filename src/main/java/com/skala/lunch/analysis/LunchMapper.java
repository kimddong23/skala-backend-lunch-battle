package com.skala.lunch.analysis;

import com.skala.lunch.analysis.dto.CategoryShareDto;
import com.skala.lunch.analysis.dto.CheerEffectDto;
import com.skala.lunch.analysis.dto.DepartmentTasteDto;
import com.skala.lunch.analysis.dto.ParticipationDto;
import com.skala.lunch.analysis.dto.PickyIndexDto;
import com.skala.lunch.analysis.dto.RankingDto;
import com.skala.lunch.analysis.dto.RestaurantSummaryDto;
import com.skala.lunch.analysis.dto.WeekdayTrendDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 집계·분석 전용 매퍼 (SQL Mapper).
 *
 * 단건 CRUD 는 JPA 가 맡고, 여러 테이블을 조인해 합계·그룹핑하는 조회만 여기서 SQL 로 처리한다.
 * 목록에 붙는 집계(평점·우승 횟수)도 여기로 옮겼다 —
 * 식당마다 따로 조회하면 식당 수만큼 질의가 늘어나기 때문이다.
 *
 * 실제 SQL 은 resources/mapper/LunchMapper.xml 에 있다.
 */
@Mapper
public interface LunchMapper {

    /** 식당 목록 + 평점·리뷰수·우승수를 한 번에. (N+1 제거용) */
    List<RestaurantSummaryDto> findRestaurantSummaries(@Param("activeOnly") boolean activeOnly);


    /** 배틀 목록 + 투표수·후보수를 한 번에. (N+1 제거용) */
    List<ParticipationDto> findBattleSummaries();

    /** 분석 1 — 식당 종합 랭킹. */
    List<RankingDto> findRanking();

    /** 분석 2 — 부서별 취향. */
    List<DepartmentTasteDto> findDepartmentTaste();

    /** 분석 3 — 요일별 인기 분류. */
    List<WeekdayTrendDto> findWeekdayTrend();

    /** 분석 4 — 개인 편식 지수. */
    List<PickyIndexDto> findPickyIndex();

    /** 분석 5 — 배틀별 투표 참여율. */
    List<ParticipationDto> findParticipation();

    /** 분석 6 — 분류별 득표 점유율. */
    List<CategoryShareDto> findCategoryShare();

    /** 응원 무용지수 — 최다 득표 메뉴가 실제로 이겼는지 배틀별로 본다. */
    List<CheerEffectDto> findCheerEffect();
}
