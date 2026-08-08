package com.skala.lunch.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * 경기장이 되는 미로.
 *
 * 같은 시드면 같은 미로가 나온다. 미로도 경기 결과의 일부이므로
 * 시드 하나로 미로·스탯·주행을 모두 재현할 수 있어야 한다.
 *
 * 만드는 방법은 재귀적 백트래킹이다. 그대로 두면 두 칸 사이의 길이 하나뿐이라
 * "최단 경로"라고 부를 것이 없어지므로, 벽을 조금 헐어 우회로를 만든다.
 * 그래야 BFS 가 여러 경로 중 진짜 최단을 고르는 일을 하게 된다.
 */
public final class Maze {

    /** 북·동·남·서 */
    public static final int N = 0, E = 1, S = 2, W = 3;

    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DY = {-1, 0, 1, 0};

    /** 벽을 헐어 우회로를 만들 비율. 0이면 길이 하나뿐인 미로가 된다. */
    private static final double LOOP_RATIO = 0.09;

    private final int cols;
    private final int rows;

    /** 칸마다 남아 있는 벽을 비트로 담는다. 비트가 서 있으면 그 방향은 막혀 있다. */
    private final int[] walls;

    public Maze(int cols, int rows, Random random) {
        this.cols = cols;
        this.rows = rows;
        this.walls = new int[cols * rows];
        Arrays.fill(walls, 0b1111);

        carve(random);
        openLoops(random);
    }

    // ── 생성 ────────────────────────────────────────────────

    /** 재귀적 백트래킹. 방문하지 않은 이웃으로 벽을 허물며 나아간다. */
    private void carve(Random random) {
        boolean[] seen = new boolean[cols * rows];
        Deque<Integer> stack = new ArrayDeque<>();

        int start = 0;
        seen[start] = true;
        stack.push(start);

        while (!stack.isEmpty()) {
            int cell = stack.peek();

            List<Integer> next = new ArrayList<>(4);
            for (int d = 0; d < 4; d++) {
                int n = neighbor(cell, d);
                if (n >= 0 && !seen[n]) {
                    next.add(d);
                }
            }

            if (next.isEmpty()) {
                stack.pop();
                continue;
            }

            int d = next.get(random.nextInt(next.size()));
            int n = neighbor(cell, d);
            breakWall(cell, d);
            seen[n] = true;
            stack.push(n);
        }
    }

    /** 벽을 조금 헐어 우회로를 만든다. */
    private void openLoops(Random random) {
        for (int cell = 0; cell < walls.length; cell++) {
            for (int d = 0; d < 4; d++) {
                if (!hasWall(cell, d)) {
                    continue;
                }
                int n = neighbor(cell, d);
                if (n >= 0 && random.nextDouble() < LOOP_RATIO) {
                    breakWall(cell, d);
                }
            }
        }
    }

    // ── 길찾기 ──────────────────────────────────────────────

    /**
     * 목적지에서 시작하는 너비 우선 탐색.
     *
     * 칸마다 목적지까지 남은 최소 걸음 수를 채운다. 이 표가 있으면
     * 어느 칸에서든 "값이 1 작은 이웃"으로 가는 것이 곧 최단 경로다.
     * 햄스터마다 매번 경로를 다시 찾을 필요가 없어진다.
     */
    public int[] distanceTo(int goal) {
        int[] dist = new int[cols * rows];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[goal] = 0;

        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(goal);

        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (int d = 0; d < 4; d++) {
                if (hasWall(cell, d)) {
                    continue;
                }
                int n = neighbor(cell, d);
                if (n >= 0 && dist[n] == Integer.MAX_VALUE) {
                    dist[n] = dist[cell] + 1;
                    queue.add(n);
                }
            }
        }
        return dist;
    }

    /** 거리표를 따라 내려가며 최단 경로를 뽑는다. */
    public List<Integer> shortestPath(int from, int goal) {
        int[] dist = distanceTo(goal);
        if (dist[from] == Integer.MAX_VALUE) {
            return List.of();
        }

        List<Integer> path = new ArrayList<>();
        int cell = from;
        path.add(cell);

        while (cell != goal) {
            int best = -1;
            for (int d = 0; d < 4; d++) {
                if (hasWall(cell, d)) {
                    continue;
                }
                int n = neighbor(cell, d);
                if (n >= 0 && dist[n] == dist[cell] - 1) {
                    best = n;
                    break;
                }
            }
            if (best < 0) {
                break;              // 거리표가 온전하면 일어나지 않는다
            }
            cell = best;
            path.add(cell);
        }
        return Collections.unmodifiableList(path);
    }

    /** 지나갈 수 있는 이웃 칸들. */
    public List<Integer> openNeighbors(int cell) {
        List<Integer> out = new ArrayList<>(4);
        for (int d = 0; d < 4; d++) {
            if (hasWall(cell, d)) {
                continue;
            }
            int n = neighbor(cell, d);
            if (n >= 0) {
                out.add(n);
            }
        }
        return out;
    }

    // ── 조회 ────────────────────────────────────────────────

    public int getCols() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public int cell(int x, int y) {
        return y * cols + x;
    }

    public boolean hasWall(int cell, int dir) {
        return (walls[cell] & (1 << dir)) != 0;
    }

    /** 화면으로 보내는 형태. 칸마다 벽 비트 하나. */
    public int[] toWallBits() {
        return walls.clone();
    }

    private int neighbor(int cell, int dir) {
        int x = cell % cols + DX[dir];
        int y = cell / cols + DY[dir];
        if (x < 0 || y < 0 || x >= cols || y >= rows) {
            return -1;
        }
        return y * cols + x;
    }

    private void breakWall(int cell, int dir) {
        int n = neighbor(cell, dir);
        if (n < 0) {
            return;
        }
        walls[cell] &= ~(1 << dir);
        walls[n] &= ~(1 << ((dir + 2) % 4));
    }
}
