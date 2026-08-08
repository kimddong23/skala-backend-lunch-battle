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
 * 햄스터 미로 경주.
 *
 * 후보로 오른 식당마다 햄스터 한 마리가 같은 미로에 들어간다.
 * 출구에 먼저 닿는 메뉴가 오늘의 점심이다.
 *
 * 승부를 가르는 것은 발이 빠른 정도가 아니라 <b>길을 얼마나 잘 찾는가</b> 다.
 * 미로의 최단 경로는 BFS 로 미리 구해 두고, 햄스터는 매 걸음
 * 그 길을 따를지(판단력) 아무 데나 갈지를 뽑는다.
 *
 * 이렇게 두는 이유가 있다. 순전히 운으로 이기는 경주라면 투표도 감점도
 * 장식일 뿐이다. 여기서는
 * <ul>
 *   <li>응원(득표)은 <b>판단력</b>을 올린다 — 표를 받은 메뉴는 길을 더 잘 안다</li>
 *   <li>최근 우승은 <b>발놀림</b>을 떨어뜨린다 — 배가 불러 굼뜨다</li>
 * </ul>
 * 둘 다 결과에 실제로 영향을 주되 결정하지는 못한다. 표를 몰아줘도
 * 길치 햄스터가 요행으로 지름길에 들어서면 뒤집힌다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    /** 미로 크기. 홀수로 두어야 바깥 벽이 반듯하게 떨어진다. */
    public static final int COLS = 21, ROWS = 11;

    /** 한 경기의 최대 걸음 수. 이 안에 못 나오면 강제 종료한다. */
    public static final int MAX_TICKS = 400;

    /** 응원이 판단력에 더할 수 있는 최대치. */
    private static final double MAX_CHEER = 0.10;

    /** 최근 우승 1회당 발놀림에서 깎는 값. */
    private static final double HANDICAP_PER_WIN = 0.06;

    // 스탯이 뽑히는 범위. 한줄평(RaceComments)이 이 값을 보고 기준을 잡는다.
    // 범위와 판정 기준을 각자 들고 있으면 한쪽만 바뀌었을 때 판정이 조용히
    // 한쪽으로 쏠린다 — 실제로 속도 평이 늘 같은 말만 나온 적이 있다.
    static final double SENSE_MIN = 0.50, SENSE_SPAN = 0.42;
    static final double PACE_MIN = 0.74, PACE_SPAN = 0.26;

    private final RaceRepository raceRepository;
    private final BattleRepository battleRepository;
    private final CandidateRepository candidateRepository;
    private final LunchMapper lunchMapper;
    private final BattleRules rules;

    /**
     * 경주를 진행한다. 배틀당 한 번만 달릴 수 있다.
     * 결과에 따라 배틀이 마감되고 우승 식당이 정해진다.
     */
    @Transactional
    public RaceDto run(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new NotFoundException("배틀을 찾을 수 없습니다: " + battleId));

        if (raceRepository.existsByBattleId(battleId)) {
            throw new ConflictException("이미 경주가 끝난 배틀입니다. 다시 하려면 초기화하세요");
        }

        List<Candidate> candidates =
                candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battleId);
        if (candidates.size() < 2) {
            throw new BadRequestException(
                    "경주는 후보가 둘 이상이어야 합니다 (현재 " + candidates.size() + "개)");
        }

        // 시드를 먼저 정해 두고 그 시드로만 난수를 뽑는다.
        // 미로까지 이 시드에서 나오므로 경기 전체를 그대로 재현할 수 있다.
        long seed = new SecureRandom().nextLong();
        Random random = new Random(seed);

        Course course = new Course(random);
        List<Runner> runners = lineUp(candidates, random);

        int totalTicks = simulate(runners, course, random);
        rank(runners, course);

        Race race = persist(battle, seed, totalTicks, runners);
        closeBattleWith(battle, runners.get(0).candidate.getRestaurant());

        log.info("[경주] 배틀 {} · {}마리 · 최단 {}칸 · {}걸음 · 우승 {}",
                battleId, runners.size(), course.optimalLength, totalTicks,
                runners.get(0).candidate.getRestaurant().getName());

        return toDto(race, runners, course, totalTicks, seed);
    }

    /**
     * 경주를 취소하고 배틀을 다시 연다 — "다시 하기".
     *
     * 후보와 표는 그대로 두고 경주 기록만 지운다. 우승도 함께 지워지므로
     * 그 우승 때문에 붙었던 감점도 같이 되돌아간다 (최근 우승 집계가
     * battles.winner_restaurant_id 를 보기 때문이다). 기록만 지우고 우승을
     * 남겨 두면 다시 달릴 때마다 없던 감점이 쌓인다.
     */
    @Transactional
    public void reset(Long battleId) {
        Battle battle = battleRepository.findById(battleId)
                .orElseThrow(() -> new NotFoundException("배틀을 찾을 수 없습니다: " + battleId));

        Race race = raceRepository.findByBattleId(battleId)
                .orElseThrow(() -> new ConflictException("아직 경주를 하지 않았습니다"));

        raceRepository.delete(race);
        raceRepository.flush();          // 유니크 제약이 걸려 있어 먼저 비워야 다시 달릴 수 있다

        battle.setStatus(Battle.Status.OPEN);
        battle.setWinner(null);
        battle.setClosedAt(null);
        battleRepository.save(battle);

        log.info("[초기화] 배틀 {} · 경주 기록 삭제, 후보와 표는 유지", battleId);
    }

    /** 이미 끝난 경주 다시 보기. */
    @Transactional(readOnly = true)
    public RaceDto replay(Long battleId) {
        Race race = raceRepository.findWithLanesByBattleId(battleId)
                .orElseThrow(() -> new NotFoundException("아직 경주를 하지 않았습니다"));

        // 저장된 시드로 미로와 주행을 그대로 다시 만든다.
        // 걸음을 전부 저장하는 대신 시드만 남겨 두는 편이 가볍다.
        Random random = new Random(race.getSeed());
        Course course = new Course(random);

        List<Runner> runners = new ArrayList<>();
        for (RaceLane lane : race.getLanes()) {
            runners.add(new Runner(lane.getCandidate(), lane.getSense(), lane.getPace(),
                    lane.getCheerBonus(), lane.getHandicap()));
        }
        // 저장된 레인은 순위 순으로 들어 있다. 주행은 출전 순서대로 난수를 뽑으므로
        // 처음 달릴 때와 같은 순서로 되돌려 놓아야 같은 경기가 재현된다.
        // 득표만으로 정렬하면 동점일 때 순서가 흔들려 다른 경기가 나온다 —
        // 실제로 재현 결과의 우승자가 달라졌다.
        runners.sort(Comparator
                .comparingInt((Runner r) -> -r.candidate.getVoteCount())
                .thenComparingLong(r -> r.candidate.getId()));

        skipStatDraws(random, runners.size());
        int totalTicks = simulate(runners, course, random);
        rank(runners, course);

        return toDto(race, runners, course, totalTicks, race.getSeed());
    }

    // ── 출전 준비 ───────────────────────────────────────────

    private List<Runner> lineUp(List<Candidate> candidates, Random random) {
        Map<Long, Long> recentWins = recentWinMap();
        int totalVotes = candidates.stream().mapToInt(Candidate::getVoteCount).sum();

        List<Runner> runners = new ArrayList<>();
        for (Candidate c : candidates) {
            long wins = recentWins.getOrDefault(c.getRestaurant().getId(), 0L);

            double cheer = totalVotes == 0 ? 0.0
                    : MAX_CHEER * c.getVoteCount() / totalVotes;
            double load = wins * HANDICAP_PER_WIN;

            double sense = SENSE_MIN + random.nextDouble() * SENSE_SPAN;
            double pace = PACE_MIN + random.nextDouble() * PACE_SPAN;

            runners.add(new Runner(c, round2(sense), round2(pace), round2(cheer), round2(load)));
        }
        return runners;
    }

    /** 스탯 뽑기에 쓰인 난수 호출 횟수만큼 건너뛴다 (재현 시 주행 난수를 맞추기 위함). */
    private void skipStatDraws(Random random, int runnerCount) {
        for (int i = 0; i < runnerCount * 2; i++) {
            random.nextDouble();
        }
    }

    // ── 주행 ────────────────────────────────────────────────

    /**
     * 한 걸음씩 진행하며 지나온 칸을 기록한다.
     *
     * 매 걸음마다
     * <ol>
     *   <li>발놀림만큼의 확률로 움직인다 (아니면 그 자리에서 머뭇거린다)</li>
     *   <li>판단력만큼의 확률로 최단 경로 쪽 칸을 고른다</li>
     *   <li>아니면 갈 수 있는 칸 중 아무 데나 고른다 — 막다른 길로 들어가기도 한다</li>
     * </ol>
     */
    private int simulate(List<Runner> runners, Course course, Random random) {
        runners.forEach(r -> {
            r.cell = course.start;
            r.finishTick = null;
            r.steps = 0;
            r.path.clear();
            r.path.add(course.start);
        });

        int tick = 0;
        while (tick < MAX_TICKS && runners.stream().anyMatch(r -> r.finishTick == null)) {
            tick++;
            for (Runner r : runners) {
                if (r.finishTick != null) {
                    r.path.add(course.goal);
                    continue;
                }

                double pace = Math.max(0.35, r.pace - r.handicap);
                if (random.nextDouble() > pace) {
                    r.path.add(r.cell);            // 머뭇거림
                    continue;
                }

                int next = chooseStep(r, course, random);
                r.previous = r.cell;
                r.cell = next;
                r.steps++;
                r.path.add(r.cell);

                if (r.cell == course.goal) {
                    r.finishTick = tick;
                }
            }
        }
        return tick;
    }

    private int chooseStep(Runner r, Course course, Random random) {
        List<Integer> open = course.maze.openNeighbors(r.cell);

        double sense = Math.min(0.97, r.sense + r.cheerBonus);
        if (random.nextDouble() < sense) {
            // 최단 경로 쪽 — 거리표 값이 가장 작은 이웃
            int best = open.get(0);
            for (int n : open) {
                if (course.dist[n] < course.dist[best]) {
                    best = n;
                }
            }
            return best;
        }

        // 헤매는 걸음. 방금 온 칸으로 곧장 되돌아가면 제자리 뜀처럼 보이므로,
        // 다른 길이 있으면 그쪽을 고른다.
        List<Integer> forward = new ArrayList<>(open);
        forward.remove(Integer.valueOf(r.previous));
        List<Integer> pick = forward.isEmpty() ? open : forward;
        return pick.get(random.nextInt(pick.size()));
    }

    private void rank(List<Runner> runners, Course course) {
        runners.sort(Comparator
                .comparing((Runner r) -> r.finishTick == null ? Integer.MAX_VALUE : r.finishTick)
                .thenComparing(r -> course.dist[r.cell]));
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
                    .sense(r.sense).pace(r.pace)
                    .cheerBonus(r.cheerBonus).handicap(r.handicap)
                    .steps(r.steps)
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

    private RaceDto toDto(Race race, List<Runner> runners, Course course,
                          int totalTicks, long seed) {

        List<RaceDto.LaneDto> lanes = runners.stream()
                .map(r -> RaceDto.LaneDto.builder()
                        .candidateId(r.candidate.getId())
                        .restaurantName(r.candidate.getRestaurant().getName())
                        .category(r.candidate.getRestaurant().getCategory().name())
                        .sense(r.sense).pace(r.pace)
                        .cheerBonus(r.cheerBonus).handicap(r.handicap)
                        .voteCount(r.candidate.getVoteCount())
                        .rank(r.rank).finishTick(r.finishTick)
                        .steps(r.steps)
                        .efficiency(r.steps == 0 ? 0.0
                                : round1(course.optimalLength * 100.0 / r.steps))
                        .path(r.path)
                        .scouting(RaceComments.scouting(r.sense, r.pace, r.cheerBonus, r.handicap))
                        .build())
                .toList();

        Runner won = runners.get(0);
        return RaceDto.builder()
                .raceId(race.getId())
                .battleId(race.getBattle().getId())
                .seed(seed)
                .cols(course.maze.getCols())
                .rows(course.maze.getRows())
                .walls(course.maze.toWallBits())
                .start(course.start)
                .goal(course.goal)
                .optimalPath(course.optimalPath)
                .optimalLength(course.optimalLength)
                .totalTicks(totalTicks)
                .winnerName(won.candidate.getRestaurant().getName())
                .headline(RaceComments.headline(won, runners, course.optimalLength))
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

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** 미로와 그 미로에서 미리 구해 둔 최단 경로. */
    static final class Course {
        final Maze maze;
        final int start;
        final int goal;
        final int[] dist;
        final List<Integer> optimalPath;
        final int optimalLength;

        Course(Random random) {
            this.maze = new Maze(COLS, ROWS, random);
            this.start = maze.cell(0, ROWS / 2);
            this.goal = maze.cell(COLS - 1, ROWS / 2);
            this.dist = maze.distanceTo(goal);
            this.optimalPath = maze.shortestPath(start, goal);
            this.optimalLength = Math.max(1, optimalPath.size() - 1);
        }
    }

    /** 미로를 달리는 햄스터 한 마리의 상태. */
    static class Runner {
        final Candidate candidate;
        final double sense, pace, cheerBonus, handicap;
        final List<Integer> path = new ArrayList<>();
        int cell;
        int previous = -1;
        int steps;
        Integer finishTick;
        int rank;

        Runner(Candidate candidate, double sense, double pace,
               double cheerBonus, double handicap) {
            this.candidate = candidate;
            this.sense = sense;
            this.pace = pace;
            this.cheerBonus = cheerBonus;
            this.handicap = handicap;
        }
    }
}
