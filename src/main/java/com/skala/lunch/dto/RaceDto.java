package com.skala.lunch.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;

import java.util.List;

/**
 * 레이스 결과 + 재생용 기록.
 *
 * 화면이 자체 난수로 애니메이션을 만들면 서버가 정한 우승과 어긋날 수 있다.
 * 서버가 계산한 매 순간의 위치를 그대로 내려보내 화면은 재생만 하게 한다.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RaceDto {

    private Long raceId;
    private Long battleId;

    /**
     * 난수 시드. 이 값으로 같은 경기를 다시 돌릴 수 있다.
     *
     * 문자열로 내보낸다. long 은 최대 19자리인데 자바스크립트 number 가 정확히
     * 담을 수 있는 정수는 9,007,199,254,740,991(16자리)까지다. 숫자로 내보내면
     * 브라우저가 끝자리를 반올림해 버려(...164683 → ...164000) 화면에 뜬 시드로는
     * 같은 경기를 재현할 수 없다. 재현성이 이 값의 존재 이유이므로 정밀도를 지킨다.
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long seed;

    private Integer trackLength;
    private Integer totalTicks;
    private String winnerName;

    /** 중계 멘트. */
    private String headline;

    private List<LaneDto> lanes;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LaneDto {
        private Long candidateId;
        private String restaurantName;
        private String category;

        /** 출발 직전에 뽑힌 스탯. */
        private Double speed;
        private Double stamina;
        private Double burst;
        private Double cheerBonus;
        private Double handicap;

        private Integer voteCount;
        private Integer rank;
        private Integer finishTick;

        /** 매 틱의 위치. 화면은 이 값을 따라 그리기만 한다. */
        private List<Integer> track;

        /** 스탯 한줄평. */
        private String scouting;
    }
}
