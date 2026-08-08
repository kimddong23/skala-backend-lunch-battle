package com.skala.lunch.service;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.CandidateDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.entity.*;
import com.skala.lunch.exception.BadRequestException;
import com.skala.lunch.exception.ConflictException;
import com.skala.lunch.exception.NotFoundException;
import com.skala.lunch.mapper.LunchMapper;
import com.skala.lunch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 점심 배틀 진행.
 *
 * 하루에 배틀 하나, 사원 한 명당 한 표.
 * 표는 그 자체로 우승을 정하지 않는다 — 미로 경주에서 판단력 가산으로 쓰인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BattleService {

    private final BattleRepository battleRepository;
    private final CandidateRepository candidateRepository;
    private final VoteRepository voteRepository;
    private final MemberRepository memberRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final BattleRules rules;
    private final LunchMapper lunchMapper;

    // ── 배틀 ────────────────────────────────────────────────

    /** 오늘의 배틀을 연다. 같은 날짜로 두 번 열 수 없다. */
    @Transactional
    public BattleDto openBattle(BattleDto request) {
        LocalDate date = request.getBattleDate();
        if (battleRepository.existsByBattleDate(date)) {
            throw new ConflictException("이미 " + date + " 배틀이 열려 있습니다");
        }

        LocalDateTime closesAt = request.getClosesAt() != null
                ? request.getClosesAt()
                : date.atTime(rules.getDefaultCloseTime());

        if (!closesAt.toLocalDate().isEqual(date)) {
            throw new BadRequestException("마감 시각은 배틀 날짜와 같은 날이어야 합니다");
        }

        // 마감이 이미 지난 배틀은 한 표도 받을 수 없다. 만들자마자 쓸모없는 배틀을 막는다.
        if (!closesAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "마감 시각이 이미 지났습니다 (" + closesAt + ")."
                            + " 지금보다 뒤인 closesAt 을 지정하세요");
        }

        Battle battle = battleRepository.save(Battle.builder()
                .battleDate(date)
                .status(Battle.Status.OPEN)
                .closesAt(closesAt)
                .build());

        return toDto(battle, true);
    }

    public BattleDto getBattle(Long id) {
        return toDto(findBattle(id), true);
    }

    public BattleDto getBattleByDate(LocalDate date) {
        Battle battle = battleRepository.findByBattleDate(date)
                .orElseThrow(() -> new NotFoundException("해당 날짜의 배틀이 없습니다: " + date));
        return toDto(battle, true);
    }

    /**
     * 배틀 목록.
     *
     * 배틀마다 투표 수를 세면 배틀 수만큼 질의가 늘어난다(N+1).
     * 목록에 필요한 집계는 SQL 한 문장으로 받아 온다.
     */
    public List<BattleDto> getAllBattles() {
        Map<Long, Battle> byId = battleRepository.findAllByOrderByBattleDateDesc().stream()
                .collect(Collectors.toMap(Battle::getId, b -> b));

        return lunchMapper.findBattleSummaries().stream()
                .map(s -> {
                    Battle b = byId.get(s.getBattleId());
                    long votes = s.getVoterCount() == null ? 0L : s.getVoterCount();
                    return BattleDto.builder()
                            .id(b.getId())
                            .battleDate(b.getBattleDate())
                            .status(b.getStatus())
                            .closesAt(b.getClosesAt())
                            .closedAt(b.getClosedAt())
                            .winnerName(s.getWinnerName())
                            .totalVotes(votes)
                            .comment(BattleComments.forBattle(b, s.getWinnerName(), votes))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteBattle(Long id) {
        Battle battle = findBattle(id);
        long votes = voteRepository.countByBattleId(id);
        long candidates = candidateRepository.countByBattleId(id);
        if (votes > 0 || candidates > 0) {
            throw new ConflictException(
                    "후보나 투표가 있는 배틀은 삭제할 수 없습니다"
                            + " (후보 " + candidates + "개, 투표 " + votes + "표)");
        }
        battleRepository.delete(battle);
    }

    // ── 후보 ────────────────────────────────────────────────

    /** 배틀에 식당을 후보로 올린다. */
    @Transactional
    public CandidateDto addCandidate(Long battleId, Long restaurantId, Long memberId) {
        Battle battle = findBattle(battleId);
        requireOpen(battle);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new NotFoundException("식당을 찾을 수 없습니다: " + restaurantId));
        if (Boolean.FALSE.equals(restaurant.getActive())) {
            throw new BadRequestException("영업하지 않는 식당은 후보로 올릴 수 없습니다: " + restaurant.getName());
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("사원을 찾을 수 없습니다: " + memberId));

        try {
            Candidate candidate = candidateRepository.save(Candidate.builder()
                    .battle(battle)
                    .restaurant(restaurant)
                    .addedBy(member)
                    .voteCount(0)
                    .build());
            return toCandidateDto(candidate, 0L);
        } catch (DataIntegrityViolationException e) {
            // 유일 제약에 걸린 것 — 동시에 같은 식당을 올린 경우도 여기로 온다
            throw new ConflictException("이미 후보에 있는 식당입니다: " + restaurant.getName());
        }
    }

    @Transactional
    public void removeCandidate(Long battleId, Long candidateId) {
        Battle battle = findBattle(battleId);
        requireOpen(battle);

        Candidate candidate = findCandidate(candidateId);
        if (!candidate.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("해당 배틀의 후보가 아닙니다");
        }

        long votes = voteRepository.countByCandidateId(candidateId);
        if (votes > 0) {
            throw new ConflictException("이미 " + votes + "표를 받은 후보는 내릴 수 없습니다");
        }
        candidateRepository.delete(candidate);
    }

    // ── 투표 ────────────────────────────────────────────────

    /**
     * 투표. 한 배틀에 한 사람은 한 표만.
     *
     * 중복 검사만으로는 동시 요청 두 개가 모두 통과할 수 있어
     * votes 테이블의 (배틀, 사원) 유일 제약이 최종 방어선이다.
     * 득표 수는 후보 행을 잠그고 올려 표가 사라지지 않게 한다.
     */
    @Transactional
    public BattleDto vote(Long battleId, VoteRequestDto request) {
        Battle battle = findBattle(battleId);
        if (!battle.isVotable()) {
            throw new BadRequestException(battle.getStatus() == Battle.Status.CLOSED
                    ? "이미 마감된 배틀입니다"
                    : "투표가 마감되었습니다 (마감 " + battle.getClosesAt() + ")");
        }

        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new NotFoundException("사원을 찾을 수 없습니다: " + request.getMemberId()));

        Candidate candidate = candidateRepository.findByIdForUpdate(request.getCandidateId())
                .orElseThrow(() -> new NotFoundException("후보를 찾을 수 없습니다: " + request.getCandidateId()));
        if (!candidate.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("해당 배틀의 후보가 아닙니다");
        }

        try {
            voteRepository.saveAndFlush(Vote.builder()
                    .battle(battle)
                    .member(member)
                    .candidate(candidate)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(member.getName() + " 님은 이미 투표했습니다 (1인 1표)");
        }

        candidate.setVoteCount(candidate.getVoteCount() + 1);
        candidateRepository.save(candidate);

        return toDto(battle, true);
    }

    /** 투표 취소. 마감 전까지만 가능하다. */
    @Transactional
    public BattleDto cancelVote(Long battleId, Long memberId) {
        Battle battle = findBattle(battleId);
        if (!battle.isVotable()) {
            throw new BadRequestException("마감된 배틀의 투표는 취소할 수 없습니다");
        }

        Vote vote = voteRepository.findByBattleIdAndMemberId(battleId, memberId)
                .orElseThrow(() -> new NotFoundException("투표 내역이 없습니다"));

        Candidate candidate = candidateRepository.findByIdForUpdate(vote.getCandidate().getId())
                .orElseThrow(() -> new NotFoundException("후보를 찾을 수 없습니다"));

        voteRepository.delete(vote);
        candidate.setVoteCount(Math.max(0, candidate.getVoteCount() - 1));
        candidateRepository.save(candidate);

        return toDto(battle, true);
    }

    // ── 마감 ────────────────────────────────────────────────

    /**
     * 마감하고 우승을 확정한다.
     *
     * 득표가 가장 많은 후보가 이긴다.
     * 동점이면 평점 높은 쪽, 그다음 가까운 쪽이 이긴다.
     *
     * 화면은 이 경로 대신 미로 경주로 마감한다. 이쪽은 "경주 없이 표로만 정하기" 를
     * 원할 때 쓰는 API 다.
     */
    @Transactional
    public BattleDto closeBattle(Long battleId) {
        Battle battle = findBattle(battleId);
        if (battle.getStatus() == Battle.Status.CLOSED) {
            throw new ConflictException("이미 마감된 배틀입니다");
        }

        List<Candidate> candidates = candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battleId);
        if (candidates.isEmpty()) {
            throw new BadRequestException("후보가 없어 마감할 수 없습니다");
        }

        Candidate winner = candidates.stream()
                .max(Comparator
                        .comparingInt(Candidate::getVoteCount)
                        .thenComparingDouble(c -> avgScore(c.getRestaurant().getId()))
                        .thenComparing(c -> -c.getRestaurant().getWalkMinutes()))
                .orElseThrow();

        battle.setStatus(Battle.Status.CLOSED);
        battle.setWinner(winner.getRestaurant());
        battle.setClosedAt(LocalDateTime.now());
        battleRepository.save(battle);

        log.info("[배틀마감] {} → {} ({}표)",
                battle.getBattleDate(), winner.getRestaurant().getName(), winner.getVoteCount());

        return toDto(battle, true);
    }

    // ── 내부 계산 ───────────────────────────────────────────

    private double avgScore(Long restaurantId) {
        return reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId).stream()
                .mapToInt(r -> r.getScore())
                .average()
                .orElse(0.0);
    }

    private Battle findBattle(Long id) {
        return battleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("배틀을 찾을 수 없습니다: " + id));
    }

    private Candidate findCandidate(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("후보를 찾을 수 없습니다: " + id));
    }

    private void requireOpen(Battle battle) {
        if (battle.getStatus() == Battle.Status.CLOSED) {
            throw new BadRequestException("마감된 배틀은 변경할 수 없습니다");
        }
    }

    // ── 변환 ────────────────────────────────────────────────

    private BattleDto toDto(Battle battle, boolean withCandidates) {
        long totalVotes = voteRepository.countByBattleId(battle.getId());

        BattleDto.BattleDtoBuilder builder = BattleDto.builder()
                .id(battle.getId())
                .battleDate(battle.getBattleDate())
                .status(battle.getStatus())
                .closesAt(battle.getClosesAt())
                .closedAt(battle.getClosedAt())
                .winnerName(battle.getWinner() == null ? null : battle.getWinner().getName())
                .totalVotes(totalVotes);

        if (withCandidates) {
            List<Candidate> candidates =
                    candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battle.getId());
            List<CandidateDto> dtos = candidates.stream()
                    .map(c -> toCandidateDto(c, totalVotes))
                    .sorted(Comparator.comparingInt(CandidateDto::getVoteCount).reversed()
                            .thenComparing(CandidateDto::getRestaurantName))
                    .collect(Collectors.toList());
            builder.candidates(dtos);
        }

        builder.comment(BattleComments.forBattle(battle,
                battle.getWinner() == null ? null : battle.getWinner().getName(),
                totalVotes));
        return builder.build();
    }

    private CandidateDto toCandidateDto(Candidate c, long totalVotes) {
        double share = totalVotes == 0 ? 0.0
                : Math.round(c.getVoteCount() * 1000.0 / totalVotes) / 10.0;

        // 지금 득표라면 경주에서 얼마를 받는지. 계산식은 RaceService 와 같아야 한다.
        double cheer = totalVotes == 0 ? 0.0
                : Math.round(RaceService.MAX_CHEER * c.getVoteCount() / totalVotes * 100.0) / 100.0;

        return CandidateDto.builder()
                .id(c.getId())
                .restaurantId(c.getRestaurant().getId())
                .restaurantName(c.getRestaurant().getName())
                .category(c.getRestaurant().getCategory())
                .walkMinutes(c.getRestaurant().getWalkMinutes())
                .price(c.getRestaurant().getPrice())
                .addedByName(c.getAddedBy().getName())
                .voteCount(c.getVoteCount())
                .sharePercent(share)
                .cheerBonus(cheer)
                .build();
    }
}
