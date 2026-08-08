package com.skala.lunch.race;

import com.skala.lunch.race.RaceDto;
import com.skala.lunch.battle.Battle;
import com.skala.lunch.battle.Candidate;
import com.skala.lunch.race.Race;
import com.skala.lunch.race.RaceLane;
import com.skala.lunch.restaurant.Restaurant;
import com.skala.lunch.global.error.BadRequestException;
import com.skala.lunch.global.error.ConflictException;
import com.skala.lunch.global.error.NotFoundException;
import com.skala.lunch.battle.BattleRepository;
import com.skala.lunch.battle.CandidateRepository;
import com.skala.lunch.race.RaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import com.skala.lunch.race.maze.Maze;

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
 * <b>득표는 경주에 아무 영향을 주지 않는다.</b> 표를 많이 받았다고 길을 더 잘 찾지 않고,
 * 스탯은 전원 같은 분포에서 뽑는다. 응원으로 유리해지면 그건 경주가 아니라 편파판정이다.
 * 표는 그날 무엇을 먹고 싶었는지의 기록으로 남아 통계(부서별 취향·편식 지수 등)에 쓰인다.
 *
 * 공정하다는 말은 주장이 아니라 측정으로 확인한다 — RaceFairnessTest 에서 한쪽에 표를
 * 몰아준 뒤 그 후보의 우승률이 1/후보수 근처에 머무는지 본다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    /** 미로 크기. 홀수로 두어야 바깥 벽이 반듯하게 떨어진다. */
    public static final int COLS = 25, ROWS = 13;

    /**
     * 한 경기의 최대 걸음 수.
     *
     * 미로가 커지면 헤매는 폭도 같이 커진다. 상한이 빠듯하면 길치 햄스터가
     * 출구를 못 찾은 채로 경기가 끝나 화면에 미완주로 남는다.
     */
    public static final int MAX_TICKS = 700;


    // 스탯이 뽑히는 범위. 한줄평(RaceComments)이 이 값을 보고 기준을 잡는다.
    // 범위와 판정 기준을 각자 들고 있으면 한쪽만 바뀌었을 때 판정이 조용히
    // 한쪽으로 쏠린다 — 실제로 속도 평이 늘 같은 말만 나온 적이 있다.
    static final double SENSE_MIN = 0.50, SENSE_SPAN = 0.42;
    static final double PACE_MIN = 0.74, PACE_SPAN = 0.26;

    private final RaceRepository raceRepository;
    private final BattleRepository battleRepository;
    private final CandidateRepository candidateRepository;

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
     * 후보와 표는 그대로 두고 경주 기록만 지운다. 우승 기록도 같이 지워
     * 배틀을 경주 전 상태로 온전히 되돌린다 — 기록만 지우고 우승을 남기면
     * 랭킹 집계에 달리지도 않은 우승이 쌓인다.
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
            runners.add(new Runner(lane.getCandidate(), lane.getSense(), lane.getPace()));
        }
        // 저장된 레인은 순위 순으로 들어 있다. 주행은 출전 순서대로 난수를 뽑으므로
        // 처음 달릴 때와 같은 순서(후보 번호 순)로 되돌려야 같은 경기가 재현된다.
        runners.sort(Comparator.comparingLong(r -> r.candidate.getId()));

        restoreDraws(random, runners);
        int totalTicks = simulate(runners, course, random);
        rank(runners, course);

        return toDto(race, runners, course, totalTicks, race.getSeed());
    }

    // ── 출전 준비 ───────────────────────────────────────────

    /**
     * 출전 준비.
     *
     * 후보 번호 순으로 세운다. 득표 순으로 세우면 표가 난수를 뽑는 차례를 바꾸게 되어,
     * 결과에 이득은 없더라도 "표가 경주를 건드린다" 는 여지가 남는다.
     */
    private List<Runner> lineUp(List<Candidate> candidates, Random random) {
        List<Runner> runners = new ArrayList<>();
        for (Candidate c : candidates.stream().sorted(Comparator.comparing(Candidate::getId)).toList()) {
            double sense = SENSE_MIN + random.nextDouble() * SENSE_SPAN;
            double pace = PACE_MIN + random.nextDouble() * PACE_SPAN;
            Runner r = new Runner(c, round2(sense), round2(pace));
            r.tiebreak = random.nextDouble();
            runners.add(r);
        }
        return runners;
    }

    /**
     * 재현할 때 출전 준비 단계의 난수를 같은 순서로 다시 뽑는다.
     *
     * 스탯은 저장된 값을 쓰므로 버리고, 동점 가르기 값만 되살린다.
     * 뽑는 횟수가 어긋나면 그 뒤 주행 난수가 통째로 밀려 다른 경기가 된다.
     */
    private void restoreDraws(Random random, List<Runner> runners) {
        for (Runner r : runners) {
            random.nextDouble();          // 판단력 (저장값 사용)
            random.nextDouble();          // 발놀림 (저장값 사용)
            r.tiebreak = random.nextDouble();
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

                if (random.nextDouble() > r.pace) {
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

        if (random.nextDouble() < r.sense) {
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

    /**
     * 순위 결정.
     *
     * 같은 걸음에 나란히 나오는 일이 드물지 않다 (60경기에 6회쯤). 이때 정렬이
     * 안정적이면 목록에 먼저 있던 쪽 — 즉 먼저 등록된 후보 — 가 늘 이긴다.
     * 실제로 동점 6회가 전부 그렇게 갈렸다. 등록 순서가 승부를 가르면 안 되므로
     * 시드에서 뽑아 둔 값으로 마지막을 가른다 (재현에도 그대로 따라온다).
     */
    private void rank(List<Runner> runners, Course course) {
        runners.sort(Comparator
                .comparing((Runner r) -> r.finishTick == null ? Integer.MAX_VALUE : r.finishTick)
                .thenComparing(r -> course.dist[r.cell])
                .thenComparingDouble(r -> r.tiebreak));
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
                        .voteCount(r.candidate.getVoteCount())
                        .rank(r.rank).finishTick(r.finishTick)
                        .steps(r.steps)
                        .efficiency(r.steps == 0 ? 0.0
                                : round1(course.optimalLength * 100.0 / r.steps))
                        .path(r.path)
                        .scouting(RaceComments.scouting(r.sense, r.pace))
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
        final double sense, pace;
        /** 같은 걸음에 들어왔을 때 순서를 가르는 값. 등록 순서가 승부를 가르지 않게 한다. */
        double tiebreak;
        final List<Integer> path = new ArrayList<>();
        int cell;
        int previous = -1;
        int steps;
        Integer finishTick;
        int rank;

        Runner(Candidate candidate, double sense, double pace) {
            this.candidate = candidate;
            this.sense = sense;
            this.pace = pace;
        }
    }
}
