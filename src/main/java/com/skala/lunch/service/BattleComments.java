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
            return "아직 아무도 응원하지 않았습니다. 첫 응원이 분위기를 만듭니다. 결과는 안 바꿉니다";
        }
        if (totalVotes < 3) {
            return "이제 " + totalVotes + "표. 판을 뒤집을 수는 없지만 마음은 전해집니다";
        }
        return totalVotes + "표 진행 중. 열심히 응원해 봐야 소용은 없습니다";
    }

    /**
     * 응원의 효능에 대한 안내.
     *
     * 득표는 경주 결과에 전혀 영향을 주지 않는다. 화면이 이걸 숨기면
     * 사용자는 표가 뭔가 하는 줄 알고 누르게 되므로, 아예 대놓고 적어 둔다.
     * 배틀 번호로 문구를 고르면 화면을 새로 고쳐도 문구가 튀지 않는다.
     */
    private static final String[] CHEER_NOTICE = {
            "응원한다고 해서 성공률이 높아지지는 않습니다. 그냥 응원하는 겁니다",
            "응원은 경주에 아무 영향이 없습니다. 그래도 하는 게 사람 마음입니다",
            "표를 아무리 몰아줘도 햄스터는 모릅니다. 햄스터는 미로만 봅니다",
            "이 표는 결과를 바꾸지 않습니다. 다만 당신의 취향으로 기록되어 통계에 남습니다",
            "응원의 효능: 없음. 그래도 응원한 메뉴가 이기면 기분은 좋습니다",
    };

    static String cheerNotice(Long battleId) {
        long i = battleId == null ? 0 : Math.abs(battleId);
        return CHEER_NOTICE[(int) (i % CHEER_NOTICE.length)];
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
