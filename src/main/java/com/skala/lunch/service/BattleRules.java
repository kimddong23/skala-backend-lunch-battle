package com.skala.lunch.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

/**
 * 배틀 운영 규칙.
 *
 * 마감 시각·감점 폭 같은 값을 코드 곳곳에 흩어 두면 규칙을 바꿀 때 다 뒤져야 한다.
 * application.yml 의 lunch.battle 아래에서 한곳으로 읽는다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "lunch.battle")
public class BattleRules {

    /** 투표 마감 시각 (기본 11:30). */
    private LocalTime defaultCloseTime = LocalTime.of(11, 30);

    /** 최근 며칠 안의 우승을 "또 그거"로 볼 것인가. */
    private int recentWindowDays = 7;

    /** 최근 우승 1회당 깎는 점수. */
    private int repeatPenalty = 2;
}
