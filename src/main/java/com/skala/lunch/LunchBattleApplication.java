package com.skala.lunch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 우리 회사 점심 정하기 — 메뉴 배틀.
 *
 * 매일 점심 후보를 올리고 사원들이 투표해 오늘의 점심을 정한다.
 * 최근에 먹은 식당은 자동으로 불이익을 받아 "또 김치찌개?" 를 막는다.
 */
@EnableScheduling
@SpringBootApplication
public class LunchBattleApplication {

    public static void main(String[] args) {
        SpringApplication.run(LunchBattleApplication.class, args);
    }
}
