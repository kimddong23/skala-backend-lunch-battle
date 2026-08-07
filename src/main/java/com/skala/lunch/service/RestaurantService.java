package com.skala.lunch.service;

import com.skala.lunch.dto.RestaurantDto;
import com.skala.lunch.entity.Restaurant;
import com.skala.lunch.exception.ConflictException;
import com.skala.lunch.exception.NotFoundException;
import com.skala.lunch.repository.BattleRepository;
import com.skala.lunch.repository.CandidateRepository;
import com.skala.lunch.repository.RestaurantRepository;
import com.skala.lunch.dto.RestaurantSummaryDto;
import com.skala.lunch.entity.Restaurant.Category;
import com.skala.lunch.mapper.LunchMapper;
import com.skala.lunch.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final CandidateRepository candidateRepository;
    private final BattleRepository battleRepository;
    private final LunchMapper lunchMapper;

    @Transactional
    public RestaurantDto create(RestaurantDto dto) {
        if (restaurantRepository.existsByName(dto.getName())) {
            throw new ConflictException("이미 등록된 식당입니다: " + dto.getName());
        }
        Restaurant saved = restaurantRepository.save(Restaurant.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .walkMinutes(dto.getWalkMinutes())
                .price(dto.getPrice())
                .active(dto.getActive() == null || dto.getActive())
                .build());
        return toDto(saved);
    }

    public RestaurantDto get(Long id) {
        return toDto(find(id));
    }

    /**
     * 전체 식당 + 집계.
     *
     * 식당마다 평점·우승을 따로 조회하면 식당 수만큼 질의가 늘어난다(N+1).
     * 집계는 SQL 한 문장으로 받아 온다.
     */
    public List<RestaurantDto> getAll() {
        return lunchMapper.findRestaurantSummaries(false).stream()
                .map(this::fromSummary).collect(Collectors.toList());
    }

    /** 후보로 올릴 수 있는 식당만. */
    public List<RestaurantDto> getActive() {
        return lunchMapper.findRestaurantSummaries(true).stream()
                .map(this::fromSummary).collect(Collectors.toList());
    }

    private RestaurantDto fromSummary(RestaurantSummaryDto s) {
        return RestaurantDto.builder()
                .id(s.getId())
                .name(s.getName())
                .category(Category.valueOf(s.getCategory()))
                .walkMinutes(s.getWalkMinutes())
                .price(s.getPrice())
                .active(s.getActive())
                .avgScore(s.getAvgScore())
                .reviewCount(s.getReviewCount())
                .winCount(s.getWinCount())
                .build();
    }

    public List<RestaurantDto> getByCategory(Restaurant.Category category) {
        return restaurantRepository.findByCategoryAndActiveTrue(category).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    /** 비 오는 날이나 회의가 빠듯할 때 쓰는 조회. */
    public List<RestaurantDto> getWithinWalk(Integer minutes) {
        return restaurantRepository.findByWalkMinutesLessThanEqualAndActiveTrue(minutes).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public RestaurantDto update(Long id, RestaurantDto dto) {
        Restaurant r = find(id);
        if (restaurantRepository.existsByNameAndIdNot(dto.getName(), id)) {
            throw new ConflictException("이미 등록된 식당입니다: " + dto.getName());
        }
        r.setName(dto.getName());
        r.setCategory(dto.getCategory());
        r.setWalkMinutes(dto.getWalkMinutes());
        r.setPrice(dto.getPrice());
        if (dto.getActive() != null) {
            r.setActive(dto.getActive());
        }
        return toDto(restaurantRepository.save(r));
    }

    /**
     * 삭제. 후보로 오른 적이 있거나 우승 이력이 있으면 지우지 않는다.
     * 지난 배틀 기록이 참조를 잃으면 통계가 깨진다.
     */
    @Transactional
    public void delete(Long id) {
        Restaurant r = find(id);
        long candidates = candidateRepository.countByRestaurantId(id);
        long wins = battleRepository.countByWinnerId(id);
        long reviews = reviewRepository.countByRestaurantId(id);
        if (candidates > 0 || wins > 0 || reviews > 0) {
            throw new ConflictException(
                    "기록이 있는 식당은 삭제할 수 없습니다. 대신 영업 종료 처리하세요"
                            + " (후보 " + candidates + "회, 우승 " + wins + "회, 리뷰 " + reviews + "건)");
        }
        restaurantRepository.delete(r);
    }

    private Restaurant find(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("식당을 찾을 수 없습니다: " + id));
    }

    private RestaurantDto toDto(Restaurant r) {
        List<Integer> scores = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(r.getId())
                .stream().map(rv -> rv.getScore()).toList();
        double avg = scores.isEmpty() ? 0.0
                : Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;

        return RestaurantDto.builder()
                .id(r.getId())
                .name(r.getName())
                .category(r.getCategory())
                .walkMinutes(r.getWalkMinutes())
                .price(r.getPrice())
                .active(r.getActive())
                .avgScore(avg)
                .reviewCount((long) scores.size())
                .winCount(battleRepository.countByWinnerId(r.getId()))
                .build();
    }
}
