-- 사원 12명 (부서별 취향 분석이 되도록 부서를 나눠 둠)
INSERT INTO members (login_id, name, department, created_at) VALUES
('kim',    '김개발', '개발팀',   CURRENT_TIMESTAMP),
('lee',    '이코딩', '개발팀',   CURRENT_TIMESTAMP),
('park',   '박배포', '개발팀',   CURRENT_TIMESTAMP),
('choi',   '최디자', '디자인팀', CURRENT_TIMESTAMP),
('jung',   '정픽셀', '디자인팀', CURRENT_TIMESTAMP),
('kang',   '강기획', '기획팀',   CURRENT_TIMESTAMP),
('cho',    '조전략', '기획팀',   CURRENT_TIMESTAMP),
('yoon',   '윤회계', '경영지원', CURRENT_TIMESTAMP),
('jang',   '장총무', '경영지원', CURRENT_TIMESTAMP),
('lim',    '임영업', '영업팀',   CURRENT_TIMESTAMP),
('shin',   '신주용', '개발팀',   CURRENT_TIMESTAMP),
('han',    '한신입', '개발팀',   CURRENT_TIMESTAMP);

-- 식당 16곳
-- 강남역 일대에서 실제로 영업 중인 식당들 (2026년 초 기준 공개 정보로 확인)
-- 도보 시간과 가격은 각 매장의 공개된 위치·대표 메뉴 기준의 대략값이다.
-- 실제 매장이므로 영업 상태·가격은 바뀔 수 있다.
INSERT INTO restaurants (name, category, walk_minutes, price, active, created_at) VALUES
('강남불백',           '한식',       5,  7000,  TRUE, CURRENT_TIMESTAMP),
('강남진해장',         '한식',       4,  11000, TRUE, CURRENT_TIMESTAMP),
('1992덮밥짜글이',     '한식',       3,  13000, TRUE, CURRENT_TIMESTAMP),
('무월식탁',           '한식',       6,  12800, TRUE, CURRENT_TIMESTAMP),
('오미라식당',         '한식',       8,  10000, TRUE, CURRENT_TIMESTAMP),
('오레노라멘',         '일식',       3,  13000, TRUE, CURRENT_TIMESTAMP),
('스시마이우',         '일식',       5,  9500,  TRUE, CURRENT_TIMESTAMP),
('도원참치',           '일식',       2,  22900, TRUE, CURRENT_TIMESTAMP),
('신복면관',           '중식',       2,  12000, TRUE, CURRENT_TIMESTAMP),
('마유유',             '중식',       5,  11000, TRUE, CURRENT_TIMESTAMP),
('고에몬',             '양식',       2,  14000, TRUE, CURRENT_TIMESTAMP),
('도치피자',           '양식',       4,  16000, TRUE, CURRENT_TIMESTAMP),
('꽃보다라면',         '분식',       7,  6500,  TRUE, CURRENT_TIMESTAMP),
('이조불쭈꾸미',       '분식',       6,  15000, TRUE, CURRENT_TIMESTAMP),
('베트남이랑',         '아시안',     5,  12900, TRUE, CURRENT_TIMESTAMP),
('호앙비엣',           '아시안',     9,  10500, TRUE, CURRENT_TIMESTAMP),
('올라포케',           '샐러드',     4,  12000, TRUE, CURRENT_TIMESTAMP),
('힘난다버거',         '패스트푸드', 4,  9400,  TRUE, CURRENT_TIMESTAMP),
('사이공본가',         '아시안',     3,  6500,  FALSE, CURRENT_TIMESTAMP);

-- 지난 배틀 5건 (랭킹·요일별·참여율 통계가 비어 보이지 않도록 이력을 깔아둠)
--
-- 우승은 경주로 정해지고 경주는 득표를 보지 않는다. 그러므로 이력도 그렇게 생겨야 한다.
-- 표를 제일 많이 받은 메뉴가 매번 이긴 이력을 깔아 두면 "응원 무용지수" 화면이
-- 적중 100% 를 가리키게 되어, 규칙과 정반대되는 이야기를 하게 된다.
-- 후보가 2~3팀이므로 최다 득표가 이길 확률은 대략 1/2~1/3 — 5건 중 2건만 맞게 둔다.
INSERT INTO battles (battle_date, status, closes_at, winner_restaurant_id, closed_at, created_at) VALUES
(DATEADD('DAY', -5, CURRENT_DATE), 'CLOSED', DATEADD('DAY', -5, CURRENT_TIMESTAMP), 3,  DATEADD('DAY', -5, CURRENT_TIMESTAMP), DATEADD('DAY', -5, CURRENT_TIMESTAMP)),
(DATEADD('DAY', -4, CURRENT_DATE), 'CLOSED', DATEADD('DAY', -4, CURRENT_TIMESTAMP), 7,  DATEADD('DAY', -4, CURRENT_TIMESTAMP), DATEADD('DAY', -4, CURRENT_TIMESTAMP)),
(DATEADD('DAY', -3, CURRENT_DATE), 'CLOSED', DATEADD('DAY', -3, CURRENT_TIMESTAMP), 5,  DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
(DATEADD('DAY', -2, CURRENT_DATE), 'CLOSED', DATEADD('DAY', -2, CURRENT_TIMESTAMP), 12, DATEADD('DAY', -2, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
(DATEADD('DAY', -1, CURRENT_DATE), 'CLOSED', DATEADD('DAY', -1, CURRENT_TIMESTAMP), 2,  DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- 평점 (식당별 편차가 생기도록) — 코멘트는 해당 식당의 대표 메뉴에 맞춘다
INSERT INTO reviews (member_id, restaurant_id, score, comment, created_at, updated_at) VALUES
(1,  1,  5, '불백정식 7천원이면 반칙이죠',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2,  1,  4, '반찬까지 풀세팅이라 든든합니다',      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3,  2,  2, '이번 주에만 세 번째 해장국입니다',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  2,  3, '24시간이라는 게 제일 큰 장점',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5,  3,  5, '된장짜글이에 밥 비비면 끝납니다',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6,  6,  5, '면 리필 무료라 점심에 과식합니다',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  6,  4, '12시 넘어 가면 줄이 깁니다',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8,  8,  4, '런치세트는 가끔 부리는 사치',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9,  13, 4, '라면에 김치볶음밥은 언제나 옳다',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 17, 2, '점심에 풀만 먹으면 힘이 안 납니다',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 5,  5, '만원에 뷔페라니 계산이 안 맞습니다',  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 15, 4, '고수 셀프바가 있어서 좋습니다',       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(1,  10, 3, '마라탕은 맵기 조절이 생명',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4,  11, 4, '명란 파스타가 생각보다 든든합니다',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7,  18, 3, '패티는 두툼한데 자리가 좁습니다',     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 지난 배틀의 후보와 투표 이력
-- 편식 지수·부서별 취향·요일별 경향이 의미를 가지려면 사원마다 표가 여러 건 있어야 한다.
INSERT INTO candidates (battle_id, restaurant_id, added_by_member_id, vote_count, created_at) VALUES
(1, 2, 1, 4, DATEADD('DAY', -5, CURRENT_TIMESTAMP)),
(1, 3, 4, 3, DATEADD('DAY', -5, CURRENT_TIMESTAMP)),
(1, 9, 10, 1, DATEADD('DAY', -5, CURRENT_TIMESTAMP)),
(2, 7, 4, 5, DATEADD('DAY', -4, CURRENT_TIMESTAMP)),
(2, 1, 6, 3, DATEADD('DAY', -4, CURRENT_TIMESTAMP)),
(3, 2, 8, 5, DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
(3, 5, 1, 4, DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
(4, 4, 1, 6, DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
(4, 12, 6, 3, DATEADD('DAY', -2, CURRENT_TIMESTAMP)),
(5, 2, 8, 5, DATEADD('DAY', -1, CURRENT_TIMESTAMP)),
(5, 3, 1, 4, DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- 투표 (1인 1표 유일 제약을 지키도록 배틀별로 사원을 나눔)
INSERT INTO votes (battle_id, member_id, candidate_id, voted_at) VALUES
(1,1,2,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),(1,2,2,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),
(1,3,2,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),(1,4,1,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),
(1,5,1,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),(1,6,1,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),
(1,7,1,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),(1,10,3,DATEADD('DAY',-5,CURRENT_TIMESTAMP)),
(2,1,5,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),(2,2,5,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),
(2,4,4,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),(2,5,4,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),
(2,6,4,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),(2,7,4,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),
(2,8,4,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),(2,11,5,DATEADD('DAY',-4,CURRENT_TIMESTAMP)),
(3,1,7,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),(3,2,7,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(3,3,7,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),(3,11,7,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(3,4,6,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),(3,5,6,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(3,8,6,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),(3,9,6,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(3,6,6,DATEADD('DAY',-3,CURRENT_TIMESTAMP)),
(4,1,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),(4,2,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(4,3,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),(4,11,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(4,12,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),(4,6,8,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(4,7,9,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),(4,8,9,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(4,9,9,DATEADD('DAY',-2,CURRENT_TIMESTAMP)),
(5,8,10,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),(5,9,10,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),
(5,6,10,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),(5,7,10,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),
(5,10,10,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),
(5,1,11,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),(5,2,11,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),
(5,3,11,DATEADD('DAY',-1,CURRENT_TIMESTAMP)),(5,11,11,DATEADD('DAY',-1,CURRENT_TIMESTAMP));
