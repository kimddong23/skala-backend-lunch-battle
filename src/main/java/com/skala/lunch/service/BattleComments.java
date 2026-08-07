package com.skala.lunch.service;

import com.skala.lunch.entity.Battle;

/**
 * 상황별 한마디.
 *
 * 응답에 섞어 넣는 문구를 서비스 곳곳에 흩어 두면 톤이 제각각이 되고 고치기도 번거롭다.
 * 문구를 여기 모아 두고 서비스는 상황만 넘긴다.
 */
final class BattleComments {

    private BattleComments() {
    }

    /**
     * 배틀 현황에 붙는 한마디.
     *
     * 우승 식당을 엔티티에서 꺼내면 목록 조회에서 배틀마다 지연 로딩이 걸린다.
     * 이름만 넘겨받아 추가 질의를 만들지 않는다.
     */
    static String forBattle(Battle battle, String winnerName, long totalVotes) {
        if (battle.getStatus() == Battle.Status.CLOSED) {
            String winner = winnerName == null ? "미정" : winnerName;
            if (totalVotes == 0) {
                return "아무도 투표하지 않아 " + winner + " 으로 정해졌습니다. 의견 없으면 따르는 겁니다";
            }
            return "오늘 점심은 " + winner + " 입니다. 이의 제기는 내일 투표로";
        }

        if (!battle.isVotable()) {
            return "투표 시간이 지났습니다. 마감을 눌러 결과를 확정하세요";
        }

        if (totalVotes == 0) {
            return "아직 아무도 투표하지 않았습니다. 첫 표가 분위기를 만듭니다";
        }
        if (totalVotes < 3) {
            return "이제 " + totalVotes + "표. 지금이라면 판을 뒤집을 수 있습니다";
        }
        return totalVotes + "표 진행 중. 눈치 보지 말고 소신 투표하세요";
    }

    /** 최근 우승 감점 안내. */
    static String penaltyNote(long recentWins, int penalty) {
        if (recentWins == 0) {
            return null;
        }
        if (recentWins == 1) {
            return "최근에 한 번 우승해서 " + penalty + "점 감점";
        }
        if (recentWins == 2) {
            return "최근 두 번이나 먹었습니다. " + penalty + "점 감점";
        }
        return "최근 " + recentWins + "번 우승. 슬슬 질릴 때도 됐습니다 (" + penalty + "점 감점)";
    }

    /** 편식 지수 판정. */
    static String pickyVerdict(double topCategoryRatio, String topCategory) {
        if (topCategoryRatio >= 0.8) {
            return topCategory + " 없이는 못 사는 사람. 다른 것도 좀 드세요";
        }
        if (topCategoryRatio >= 0.6) {
            return topCategory + " 편애가 뚜렷합니다";
        }
        if (topCategoryRatio >= 0.4) {
            return topCategory + " 을(를) 좋아하지만 골고루 먹는 편";
        }
        return "취향이 넓습니다. 뭘 먹자고 해도 따라올 사람";
    }
}
