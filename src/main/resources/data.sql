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
INSERT INTO restaurants (name, category, walk_minutes, price, active, created_at) VALUES
('할매국밥',           '한식',       3,  9000,  TRUE, CURRENT_TIMESTAMP),
('김치찌개의민족',     '한식',       5,  8500,  TRUE, CURRENT_TIMESTAMP),
('성수동돈까스',       '일식',       7,  12000, TRUE, CURRENT_TIMESTAMP),
('마라공장',           '중식',       6,  13000, TRUE, CURRENT_TIMESTAMP),
('회전초밥천국',       '일식',       12, 18000, TRUE, CURRENT_TIMESTAMP),
('파스타는사랑',       '양식',       9,  15000, TRUE, CURRENT_TIMESTAMP),
('분식왕떡볶이',       '분식',       2,  7000,  TRUE, CURRENT_TIMESTAMP),
('쌀국수한그릇',       '아시안',     8,  11000, TRUE, CURRENT_TIMESTAMP),
('샐러디샐러드',       '샐러드',     4,  10000, TRUE, CURRENT_TIMESTAMP),
('버거인더하우스',     '패스트푸드', 5,  9500,  TRUE, CURRENT_TIMESTAMP),
('짜장면vs짬뽕',       '중식',       10, 9000,  TRUE, CURRENT_TIMESTAMP),
('제육의정석',         '한식',       4,  9500,  TRUE, CURRENT_TIMESTAMP),
('규동집',             '일식',       6,  11000, TRUE, CURRENT_TIMESTAMP),
('타코형제',           '양식',       11, 13500, TRUE, CURRENT_TIMESTAMP),
('국수나무그늘',       '한식',       3,  8000,  TRUE, CURRENT_TIMESTAMP),
('사장님이미쳤어요',   '한식',       15, 6000,  FALSE, CURRENT_TIMESTAMP);

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

-- 평점 (식당별 편차가 생기도록)
INSERT INTO reviews (member_id, restaurant_id, score, comment, created_at, updated_at) VALUES
(1, 1, 5, '국밥은 진리입니다',           CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 1, 4, '뜨끈하고 좋음',               CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, 2, 2, '이번 주에만 세 번째입니다',   CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, 2, 3, '무난',                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, 3, 5, '돈까스는 배신하지 않는다',    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, 4, 5, '마라탕 없인 못 살아',         CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(7, 4, 1, '너무 매워서 오후 일을 못했다', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(8, 5, 4, '가끔은 사치도 필요',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9, 7, 4, '떡볶이는 언제나 옳다',        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(10, 9, 2, '점심에 풀만 먹으면 힘이 안 난다', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(11, 3, 5, '성수동돈까스 최고',          CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(12, 12, 4, '제육 맛집 인정',            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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
