package com.skala.lunch.service;

import java.util.List;

/**
 * 경주 중계 멘트.
 *
 * 스탯이 매번 달라지므로 상황을 보고 문구를 고른다.
 * 서비스 코드에 문자열이 섞이면 톤이 흐트러져 여기 모아 둔다.
 */
final class RaceComments {

    private RaceComments() {
    }

    /** 경기 총평. */
    static String headline(RaceService.Runner winner, List<RaceService.Runner> all,
                           int optimalLength) {
        String name = winner.candidate.getRestaurant().getName();
        int votes = winner.candidate.getVoteCount();
        int topVotes = all.stream().mapToInt(r -> r.candidate.getVoteCount()).max().orElse(0);

        if (winner.steps <= optimalLength) {
            return name + " 우승. 한 걸음도 헤매지 않고 최단 경로만 밟았습니다";
        }
        if (winner.handicap > 0) {
            return name + " 우승. 배가 부른 채로도 길을 찾아냈습니다. 또 먹으라는 뜻인가 봅니다";
        }
        if (votes == 0) {
            return name + " 우승. 한 표도 못 받은 햄스터가 출구를 먼저 찾았습니다";
        }
        if (votes < topVotes) {
            return name + " 우승. 표는 다른 데 몰렸는데 길은 이쪽이 더 잘 알았습니다";
        }
        if (all.size() >= 3 && closeFinish(all)) {
            return name + " 우승. 마지막 갈림길에서 갈렸습니다";
        }
        if (winner.steps > optimalLength * 2) {
            return name + " 우승. 실컷 헤매고도 1등입니다. 다들 그만큼 헤맸다는 뜻입니다";
        }
        return name + " 우승. 갈림길마다 옳은 쪽을 골랐습니다";
    }

    private static boolean closeFinish(List<RaceService.Runner> all) {
        Integer first = all.get(0).finishTick;
        Integer second = all.get(1).finishTick;
        return first != null && second != null && second - first <= 3;
    }

    /**
     * 출전 전 스탯 한줄평.
     *
     * 판정 기준은 실제로 스탯이 뽑히는 범위에서 잡는다. 고정 숫자를 쓰면
     * 범위 밖에 놓여 한 갈래만 나오게 된다 — 속도 기준이 2.4/1.9 였을 때
     * 최저 속도가 3.0 이라 모든 햄스터가 "발이 빠릅니다" 한 마디만 받았다.
     */
    static String scouting(double sense, double pace, double cheerBonus, double handicap) {
        StringBuilder sb = new StringBuilder();

        double sharp = RaceService.SENSE_MIN + RaceService.SENSE_SPAN * 2 / 3.0;
        double dull = RaceService.SENSE_MIN + RaceService.SENSE_SPAN / 3.0;
        if (sense >= sharp) sb.append("길눈이 밝습니다");
        else if (sense >= dull) sb.append("웬만하면 찾아갑니다");
        else sb.append("막다른 길을 좋아합니다");

        double quick = RaceService.PACE_MIN + RaceService.PACE_SPAN * 2 / 3.0;
        double slow = RaceService.PACE_MIN + RaceService.PACE_SPAN / 3.0;
        if (pace >= quick) sb.append(" · 발이 부지런합니다");
        else if (pace <= slow) sb.append(" · 자주 멈춰 섭니다");

        if (cheerBonus > 0) sb.append(" · 응원을 업었습니다");
        if (handicap > 0) sb.append(" · 배가 불렀습니다");

        return sb.toString();
    }
}
