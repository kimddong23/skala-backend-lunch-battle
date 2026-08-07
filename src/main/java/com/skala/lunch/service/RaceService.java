package com.skala.lunch.service;

import com.skala.lunch.dto.RaceDto;
import com.skala.lunch.entity.*;
import com.skala.lunch.exception.BadRequestException;
import com.skala.lunch.exception.ConflictException;
import com.skala.lunch.exception.NotFoundException;
import com.skala.lunch.mapper.LunchMapper;
import com.skala.lunch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;

/**
 * 햄스터 레이스.
 *
 * 후보로 오른 식당마다 햄스터 한 마리가 달린다. 먼저 결승선을 넘는 쪽이 오늘의 점심이다.
 *
 * 스탯은 출발 직전에 뽑는다. 만든 사람도 결과를 미리 알 수 없다.
 * 득표는 약간의 가산(응원), 최근 우승은 약간의 감산(짐)일 뿐이고
 * 승부는 대체로 그날의 운이 가른다 — 그래야 표를 몰아줘도 뒤집히는 재미가 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    /** 응원(득표)이 줄 수 있는 최대 가산. 승부를 뒤집을 만큼 크지 않게 둔다. */
    private static final double MAX_CHEER = 0.55;

    /** 최근 우승 1회당 짊어지는 짐. */
    private static final double HANDICAP_PER_WIN = 0.22;

    private final RaceRepository raceRepository;
    private final BattleRepository battleRepository;
    private final CandidateRepository candidateRepository;
    private final LunchMapper lunchMapper;
    private final BattleRules rules;

    /**
     * 레이스를 진행한다. 배틀당 한 번만 달릴 수 있다.
     * 결과에 따라 배틀이 마감되고 우승 식당이 정해진다.
     */
    @Transactional
    public RaceDto run(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new NotFoundException("배틀을 찾을 수 없습니다: " + battleId));

        if (raceRepository.existsByBattleId(battleId)) {
            throw new ConflictException("이미 레이스가 끝난 배틀입니다. 결과 조회를 쓰세요");
        }

        List<Candidate> candidates =
                candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battleId);
        if (candidates.size() < 2) {
            throw new BadRequestException(
                    "레이스는 후보가 둘 이상이어야 합니다 (현재 " + candidates.size() + "개)");
        }

        // 시드를 먼저 정해 두고 그 시드로만 난수를 뽑는다.
        // 이렇게 해야 같은 경기를 나중에 그대로 재현할 수 있다.
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);

        Map<Long, Long> recentWins = recentWinMap();
        int totalVotes = candidates.stream().mapToInt(Candidate::getVoteCount).sum();

        List<Runner> runners = new ArrayList<>();
        for (Candidate c : candidates) {
            long wins = recentWins.getOrDefault(c.getRestaurant().getId(), 0L);
            double cheer = totalVotes == 0 ? 0.0
                    : MAX_CHEER * c.getVoteCount() / totalVotes;

            runners.add(new Runner(
                    c,
                    round2(3.0 + random.nextDouble() * 3.4),          // 속도 3.0 ~ 6.4
                    round2(0.55 + random.nextDouble() * 0.45),        // 지구력 0.55 ~ 1.0
                    round2(0.02 + random.nextDouble() * 0.13),        // 순간가속 확률 2 ~ 15%
                    round2(cheer),
                    round2(wins * HANDICAP_PER_WIN)));
        }

        int totalTicks = simulate(runners, random);
        rank(runners);

        Race race = persist(battle, seed, totalTicks, runners);
        closeBattleWith(battle, runners.get(0).candidate.getRestaurant());

        log.info("[레이스] 배틀 {} · {}마리 · {}틱 · 우승 {}",
                battleId, runners.size(), totalTicks,
                runners.get(0).candidate.getRestaurant().getName());

        return toDto(race, runners, totalTicks, seed);
    }

    /** 이미 끝난 레이스 다시 보기. */
    @Transactional(readOnly = true)
    public RaceDto replay(Long battleId) {
        Race race = raceRepository.findWithLanesByBattleId(battleId)
                .orElseThrow(() -> new NotFoundException("아직 레이스를 하지 않았습니다"));

        // 저장된 시드로 같은 경기를 그대로 다시 만든다.
        // 틱마다의 위치를 전부 저장하는 대신 시드만 남겨 두는 편이 가볍다.
        Random random = new Random(race.getSeed());
        List<Runner> runners = new ArrayList<>();
        for (RaceLane lane : race.getLanes()) {
            runners.add(new Runner(lane.getCandidate(), lane.getSpeed(), lane.getStamina(),
                    lane.getBurst(), lane.getCheerBonus(), lane.getHandicap()));
        }
        runners.sort(Comparator.comparingLong(r -> r.candidate.getId()));

        // 스탯은 저장된 값을 쓰고, 주행 난수만 같은 시드로 재현한다
        skipStatDraws(random, runners.size());
        int totalTicks = simulate(runners, random);
        rank(runners);

        return toDto(race, runners, totalTicks, race.getSeed());
    }

    // ── 경주 ────────────────────────────────────────────────

    /**
     * 한 틱씩 진행하며 위치를 기록한다.
     *
     * 매 틱 이동량 = (속도 + 응원 - 짐) x 후반 체력 x 흔들림 (+ 가끔 터지는 가속)
     * 흔들림이 크기 때문에 스탯이 좋아도 질 수 있다.
     */
    private int simulate(List<Runner> runners, Random random) {
        runners.forEach(r -> {
            r.position = 0;
            r.finishTick = null;
            r.track.clear();
            r.track.add(0);
        });

        int tick = 0;
        while (tick < Race.MAX_TICKS && runners.stream().anyMatch(r -> r.finishTick == null)) {
            tick++;
            for (Runner r : runners) {
                if (r.finishTick != null) {
                    r.track.add(Race.TRACK_LENGTH);
                    continue;
                }

                // 지구력이 낮으면 후반에 처지지만, 완주는 하도록 감쇠 폭을 제한한다.
                // 트랙 위에 멈춰 선 햄스터는 화면에서 고장으로 보인다.
                double fatigue = 1.0 - (1.0 - r.stamina) * Math.min(1.0, tick / 260.0) * 0.45;
                double jitter = 0.55 + random.nextDouble() * 0.9;      // 0.55 ~ 1.45
                // 짐이 속도를 넘어서면 이동량이 0 이하가 되어 트랙 위에 멈춘다.
                // 아무리 무거워도 절반 아래로는 떨어지지 않게 막는다.
                double effective = Math.max(r.speed * 0.5, r.speed + r.cheerBonus - r.handicap);
                double step = effective * Math.max(0.25, fatigue) * jitter;

                if (random.nextDouble() < r.burst) {
                    step *= 2.0 + random.nextDouble() * 2.0;           // 순간 가속
                }

                r.position += Math.max(0.1, step);
                if (r.position >= Race.TRACK_LENGTH) {
                    r.position = Race.TRACK_LENGTH;
                    r.finishTick = tick;
                }
                r.track.add((int) Math.round(r.position));
            }
        }
        return tick;
    }

    /** 스탯 뽑기에 쓰인 난수 호출 횟수만큼 건너뛴다 (재현 시 주행 난수를 맞추기 위함). */
    private void skipStatDraws(Random random, int runnerCount) {
        for (int i = 0; i < runnerCount * 3; i++) {
            random.nextDouble();
        }
    }

    private void rank(List<Runner> runners) {
        runners.sort(Comparator
                .comparing((Runner r) -> r.finishTick == null ? Integer.MAX_VALUE : r.finishTick)
                .thenComparing(r -> -r.position));
        for (int i = 0; i < runners.size(); i++) {
            runners.get(i).rank = i + 1;
        }
    }

    // ── 저장·변환 ───────────────────────────────────────────

    private Race persist(Battle battle, long seed, int totalTicks, List<Runner> runners) {
        Race race = Race.builder()
                .battle(battle)
                .seed(seed)
                .totalTicks(totalTicks)
                .winner(runners.get(0).candidate.getRestaurant())
                .build();

        for (Runner r : runners) {
            race.getLanes().add(RaceLane.builder()
                    .race(race)
                    .candidate(r.candidate)
                    .speed(r.speed).stamina(r.stamina).burst(r.burst)
                    .cheerBonus(r.cheerBonus).handicap(r.handicap)
                    .finishTick(r.finishTick).rank(r.rank)
                    .build());
        }
        return raceRepository.save(race);
    }

    private void closeBattleWith(Battle battle, Restaurant winner) {
        battle.setStatus(Battle.Status.CLOSED);
        battle.setWinner(winner);
        battle.setClosedAt(java.time.LocalDateTime.now());
        battleRepository.save(battle);
    }

    private RaceDto toDto(Race race, List<Runner> runners, int totalTicks, long seed) {
        List<RaceDto.LaneDto> lanes = runners.stream()
                .map(r -> RaceDto.LaneDto.builder()
                        .candidateId(r.candidate.getId())
                        .restaurantName(r.candidate.getRestaurant().getName())
                        .category(r.candidate.getRestaurant().getCategory().name())
                        .speed(r.speed).stamina(r.stamina).burst(r.burst)
                        .cheerBonus(r.cheerBonus).handicap(r.handicap)
                        .voteCount(r.candidate.getVoteCount())
                        .rank(r.rank).finishTick(r.finishTick)
                        .track(r.track)
                        .scouting(RaceComments.scouting(r.speed, r.stamina, r.burst, r.handicap))
                        .build())
                .toList();

        Runner won = runners.get(0);
        return RaceDto.builder()
                .raceId(race.getId())
                .battleId(race.getBattle().getId())
                .seed(seed)
                .trackLength(Race.TRACK_LENGTH)
                .totalTicks(totalTicks)
                .winnerName(won.candidate.getRestaurant().getName())
                .headline(RaceComments.headline(won, runners))
                .lanes(lanes)
                .build();
    }

    private Map<Long, Long> recentWinMap() {
        LocalDate from = LocalDate.now().minusDays(rules.getRecentWindowDays());
        Map<Long, Long> map = new HashMap<>();
        lunchMapper.findRecentWinCounts(from, LocalDate.now()).forEach(r ->
                map.put(r.getRestaurantId(), r.getWinCount() == null ? 0L : r.getWinCount()));
        return map;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** 경주 중인 햄스터 한 마리의 상태. */
    static class Runner {
        final Candidate candidate;
        final double speed, stamina, burst, cheerBonus, handicap;
        final List<Integer> track = new ArrayList<>();
        double position;
        Integer finishTick;
        int rank;

        Runner(Candidate candidate, double speed, double stamina,
               double burst, double cheerBonus, double handicap) {
            this.candidate = candidate;
            this.speed = speed;
            this.stamina = stamina;
            this.burst = burst;
            this.cheerBonus = cheerBonus;
            this.handicap = handicap;
        }
    }
}
