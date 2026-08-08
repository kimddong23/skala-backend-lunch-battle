package com.skala.lunch.analysis;

/**
 * 분석 결과에 붙는 한마디.
 *
 * 편식 지수는 숫자만 보여 주면 그래서 어떻다는 것인지 알기 어렵다.
 * 비중을 사람이 읽을 수 있는 판정으로 바꿔 준다.
 */
final class AnalysisComments {

    private AnalysisComments() {
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
