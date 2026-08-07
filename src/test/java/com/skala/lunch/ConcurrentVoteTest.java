package com.skala.lunch;

import com.skala.lunch.dto.BattleDto;
import com.skala.lunch.dto.VoteRequestDto;
import com.skala.lunch.repository.*;
import com.skala.lunch.service.BattleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 투표 검증.
 *
 * 이 클래스만 @Transactional 을 붙이지 않는다. 트랜잭션 안에서 돌리면 스레드마다
 * 트랜잭션이 갈라지지 않아 동시성 문제가 재현되지 않기 때문이다.
 * 대신 전용 배틀을 만들고 끝나면 직접 정리한다.
 */
@SpringBootTest
@DisplayName("동시 투표")
class ConcurrentVoteTest {

    @Autowired BattleService battleService;
    @Autowired BattleRepository battleRepository;
    @Autowired CandidateRepository candidateRepository;
    @Autowired VoteRepository voteRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired VoteAuditLogRepository auditRepository;

    private Long battleId;
    private Long candidateA;
    private Long candidateB;

    @BeforeEach
    void 전용_배틀_준비() {
        // data.sql 과 겹치지 않는 미래 날짜
        LocalDate date = LocalDate.now().plusDays(30);
        battleRepository.findByBattleDate(date).ifPresent(b -> {
            voteRepository.deleteAll(voteRepository.findByBattleId(b.getId()));
            candidateRepository.deleteAll(candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(b.getId()));
            battleRepository.delete(b);
        });

        BattleDto battle = battleService.openBattle(BattleDto.builder()
                .battleDate(date)
                .closesAt(LocalDateTime.of(date, java.time.LocalTime.of(23, 59)))
                .build());
        battleId = battle.getId();
        candidateA = battleService.addCandidate(battleId, 1L, 1L).getId();
        candidateB = battleService.addCandidate(battleId, 3L, 1L).getId();
    }

    @AfterEach
    void 정리() {
        auditRepository.deleteAll(auditRepository.findByBattleId(battleId,
                org.springframework.data.domain.Sort.unsorted()));
        voteRepository.deleteAll(voteRepository.findByBattleId(battleId));
        candidateRepository.deleteAll(
                candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battleId));
        battleRepository.deleteById(battleId);
    }

    @Test
    @DisplayName("같은 사람이 동시에 20번 눌러도 한 표만 들어간다")
    void 같은_사람_연타() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        runTogether(20, i -> {
            try {
                battleService.vote(battleId, VoteRequestDto.builder()
                        .memberId(1L).candidateId(i % 2 == 0 ? candidateA : candidateB).build());
                accepted.incrementAndGet();
            } catch (Exception e) {
                rejected.incrementAndGet();
            }
        });

        // 유일 제약이 없으면 여러 요청이 모두 통과해 표가 부풀려진다
        assertThat(accepted.get()).as("한 표만 받아들여짐").isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(19);
        assertThat(voteRepository.countByBattleId(battleId)).isEqualTo(1);
        assertThat(totalCandidateVotes()).as("집계도 1")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("서로 다른 12명이 동시에 투표해도 표가 사라지지 않는다")
    void 여러_사람_동시_투표() throws Exception {
        List<Long> memberIds = memberRepository.findAll().stream()
                .map(m -> m.getId()).limit(12).toList();
        AtomicInteger accepted = new AtomicInteger();

        runTogether(memberIds.size(), i -> {
            try {
                battleService.vote(battleId, VoteRequestDto.builder()
                        .memberId(memberIds.get(i))
                        .candidateId(i % 2 == 0 ? candidateA : candidateB).build());
                accepted.incrementAndGet();
            } catch (Exception ignored) {
            }
        });

        // 잠금이 없으면 동시 갱신이 서로를 덮어써 집계가 실제 표보다 적어진다
        long votes = voteRepository.countByBattleId(battleId);
        assertThat(accepted.get()).isEqualTo(memberIds.size());
        assertThat(votes).as("투표 기록").isEqualTo(memberIds.size());
        assertThat(totalCandidateVotes()).as("후보 집계 = 실제 표").isEqualTo(votes);
    }

    @Test
    @DisplayName("같은 식당을 동시에 후보로 올려도 하나만 등록된다")
    void 동시_후보_등록() throws Exception {
        AtomicInteger ok = new AtomicInteger();

        runTogether(10, i -> {
            try {
                battleService.addCandidate(battleId, 5L, 1L);
                ok.incrementAndGet();
            } catch (Exception ignored) {
            }
        });

        assertThat(ok.get()).as("한 번만 등록").isEqualTo(1);
        assertThat(candidateRepository.countByBattleId(battleId))
                .as("기존 2개 + 새로 1개").isEqualTo(3);
    }

    private long totalCandidateVotes() {
        return candidateRepository.findByBattleIdOrderByVoteCountDescIdAsc(battleId).stream()
                .mapToLong(c -> c.getVoteCount()).sum();
    }

    /** 지정한 수만큼 스레드를 만들어 동시에 출발시킨다. */
    private void runTogether(int threads, java.util.function.IntConsumer task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await();
                    task.accept(idx);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).as("30초 안에 완료").isTrue();
        pool.shutdown();
    }
}
