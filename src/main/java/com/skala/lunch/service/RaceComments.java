package com.skala.lunch.service;

import java.util.List;

/**
 * 레이스 중계 멘트.
 *
 * 스탯이 매번 달라지므로 상황을 보고 문구를 고른다.
 * 서비스 코드에 문자열이 섞이면 톤이 흐트러져 여기 모아 둔다.
 */
final class RaceComments {

    private RaceComments() {
    }

    /** 경기 총평. */
    static String headline(RaceService.Runner winner, List<RaceService.Runner> all) {
        String name = winner.candidate.getRestaurant().getName();
        int votes = winner.candidate.getVoteCount();
        int topVotes = all.stream().mapToInt(r -> r.candidate.getVoteCount()).max().orElse(0);

        if (winner.handicap > 0) {
            return name + " 우승. 짐을 지고도 이겼습니다. 오늘은 먹으라는 뜻인가 봅니다";
        }
        if (votes == 0) {
            return name + " 우승. 한 표도 못 받은 햄스터가 결승선을 먼저 넘었습니다";
        }
        if (votes < topVotes) {
            return name + " 우승. 표는 다른 데 몰렸는데 결과는 이렇게 됐습니다";
        }
        if (all.size() >= 3 && closeFinish(all)) {
            return name + " 우승. 마지막까지 몰렸던 접전이었습니다";
        }
        return name + " 우승. 출발부터 끝까지 앞섰습니다";
    }

    private static boolean closeFinish(List<RaceService.Runner> all) {
        Integer first = all.get(0).finishTick;
        Integer second = all.get(1).finishTick;
        return first != null && second != null && second - first <= 3;
    }

    /** 출전 전 스탯 한줄평. */
    static String scouting(double speed, double stamina, double burst, double handicap) {
        StringBuilder sb = new StringBuilder();

        if (speed >= 2.4) sb.append("발이 빠릅니다");
        else if (speed >= 1.9) sb.append("평균은 합니다");
        else sb.append("느긋합니다");

        if (stamina >= 0.85) sb.append(" · 끝까지 버팁니다");
        else if (stamina <= 0.6) sb.append(" · 후반에 지칩니다");

        if (burst >= 0.12) sb.append(" · 갑자기 튀어나갑니다");

        if (handicap > 0) sb.append(" · 짐을 졌습니다");

        return sb.toString();
    }
}
