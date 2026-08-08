package com.skala.lunch;

import com.skala.lunch.service.Maze;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미로와 최단 경로.
 *
 * 경주의 승부가 "최단 경로를 얼마나 잘 따라가는가" 로 갈리므로,
 * 그 최단 경로가 정말 최단인지부터 확인해야 한다. 여기가 틀리면
 * 판단력이 높은 햄스터가 오히려 먼 길로 성실하게 걸어간다.
 */
@DisplayName("미로")
class MazeTest {

    private static final int COLS = 21, ROWS = 11;

    @Test
    @DisplayName("모든 칸에 갈 수 있다 — 끊긴 구역이 생기면 햄스터가 갇힌다")
    void 전부_이어져_있다() {
        for (int s = 0; s < 40; s++) {
            Maze maze = new Maze(COLS, ROWS, new Random(s));
            int[] dist = maze.distanceTo(maze.cell(COLS - 1, ROWS / 2));

            long unreachable = Arrays.stream(dist).filter(d -> d == Integer.MAX_VALUE).count();
            assertThat(unreachable).as("시드 %d 에서 닿을 수 없는 칸".formatted(s)).isZero();
        }
    }

    @Test
    @DisplayName("거리표가 실제 최단 거리와 일치한다 — 직접 BFS 로 다시 세어 확인")
    void 거리표가_맞다() {
        for (int s = 0; s < 20; s++) {
            Maze maze = new Maze(COLS, ROWS, new Random(s));
            int goal = maze.cell(COLS - 1, ROWS / 2);

            int[] fromMaze = maze.distanceTo(goal);
            int[] independent = bruteForceBfs(maze, goal);

            assertThat(fromMaze).as("시드 %d".formatted(s)).isEqualTo(independent);
        }
    }

    @Test
    @DisplayName("최단 경로는 실제로 이어져 있고 길이가 거리표와 같다")
    void 최단경로가_유효하다() {
        for (int s = 0; s < 30; s++) {
            Maze maze = new Maze(COLS, ROWS, new Random(s));
            int start = maze.cell(0, ROWS / 2);
            int goal = maze.cell(COLS - 1, ROWS / 2);

            List<Integer> path = maze.shortestPath(start, goal);

            assertThat(path).as("경로가 비어 있지 않다").isNotEmpty();
            assertThat(path.get(0)).isEqualTo(start);
            assertThat(path.get(path.size() - 1)).isEqualTo(goal);
            assertThat(path.size() - 1)
                    .as("경로 길이가 거리표 값과 같다")
                    .isEqualTo(maze.distanceTo(goal)[start]);

            // 이웃한 칸끼리 벽 없이 이어져 있는지
            for (int i = 0; i + 1 < path.size(); i++) {
                assertThat(maze.openNeighbors(path.get(i)))
                        .as("%d번째 걸음이 벽을 통과한다".formatted(i))
                        .contains(path.get(i + 1));
            }
            // 같은 칸을 두 번 밟지 않는다
            assertThat(new HashSet<>(path)).as("경로에 되돌아감이 없다").hasSize(path.size());
        }
    }

    @Test
    @DisplayName("우회로가 있다 — 길이 하나뿐이면 최단 경로를 고를 일이 없다")
    void 우회로가_있다() {
        int withChoice = 0;

        for (int s = 0; s < 30; s++) {
            Maze maze = new Maze(COLS, ROWS, new Random(s));
            int goal = maze.cell(COLS - 1, ROWS / 2);
            int[] dist = maze.distanceTo(goal);

            // 최단 경로 쪽 이웃이 둘 이상인 칸이 있으면 갈림길이 존재한다
            for (int c = 0; c < COLS * ROWS && withChoice <= s; c++) {
                final int cell = c;
                long best = maze.openNeighbors(c).stream()
                        .filter(n -> dist[n] == dist[cell] - 1).count();
                if (best > 1) {
                    withChoice++;
                    break;
                }
            }
        }

        System.out.println("── 갈림길이 있는 미로: " + withChoice + "/30");
        assertThat(withChoice).as("대부분의 미로에 우회로가 있어야 한다").isGreaterThan(15);
    }

    @Test
    @DisplayName("같은 시드면 같은 미로가 나온다")
    void 재현성() {
        Maze a = new Maze(COLS, ROWS, new Random(12345L));
        Maze b = new Maze(COLS, ROWS, new Random(12345L));
        Maze c = new Maze(COLS, ROWS, new Random(54321L));

        assertThat(a.toWallBits()).isEqualTo(b.toWallBits());
        assertThat(a.toWallBits()).isNotEqualTo(c.toWallBits());
    }

    /** 미로 코드와 따로 짠 BFS. 같은 답이 나와야 거리표를 믿을 수 있다. */
    private int[] bruteForceBfs(Maze maze, int goal) {
        int[] dist = new int[COLS * ROWS];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[goal] = 0;

        Deque<Integer> queue = new ArrayDeque<>(List.of(goal));
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (int n : maze.openNeighbors(cell)) {
                if (dist[n] == Integer.MAX_VALUE) {
                    dist[n] = dist[cell] + 1;
                    queue.add(n);
                }
            }
        }
        return dist;
    }
}
