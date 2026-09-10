package com.kingdom.game.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * WaveManager —— 只负责"按出怪队列逐只到点出怪"的节奏控制。
 *
 * 出怪队列由 GameController 在 beginWave 时传入（规范 §3.3 时间轴展开），
 * 每只指令含敌人 id 与相对本波开始的绝对时刻；敌人类型/数值均不在此写死
 * （id 词汇表见 LevelWaves.VOCABULARY，数值归 GameConfig 按 id 补齐）。
 */
public class WaveManager {

    /** 一只敌人的出怪指令：type = 敌人 id；offsetNanos = 相对本波开始的出生时刻（纳秒） */
    public static final class SpawnOrder {
        public final String type;
        public final long offsetNanos;

        public SpawnOrder(String type, long offsetNanos) {
            this.type = type;
            this.offsetNanos = offsetNanos;
        }
    }

    private final List<SpawnOrder> orders = new ArrayList<>();
    private long waveStartNanos = -1;
    private int spawned = 0;
    private boolean spawning = false;

    /** 开始一波（出怪队列版：由 GameController 把该波 groups 按 §3.3 展开后调用） */
    public void beginWave(List<SpawnOrder> orders) {
        this.orders.clear();
        if (orders != null) this.orders.addAll(orders);
        this.spawned = 0;
        this.waveStartNanos = System.nanoTime();
        this.spawning = true;
    }

    /**
     * 旧签名便捷入口（无波次表时的回退路径）：等价于 enemyCount 只普通敌人、
     * 统一 spawnIntervalMs 间隔的出怪队列（与旧的 spawned×interval 公式逐毫秒一致）。
     * 注意 "normal_enemy" 须与 LevelWaves.VOCABULARY 中普通敌人 id 一致。
     */
    public void beginWave(int enemyCount, long spawnIntervalMs) {
        List<SpawnOrder> uniform = new ArrayList<>(Math.max(0, enemyCount));
        long stepNanos = Math.max(0, spawnIntervalMs) * 1_000_000L;
        for (int i = 0; i < enemyCount; i++) {
            uniform.add(new SpawnOrder("normal_enemy", i * stepNanos));
        }
        beginWave(uniform);
    }

    /**
     * 每帧推进：到达指令时刻则调用 spawnAction 生成对应类型敌人。
     *
     * @param nowNanos    当前系统纳秒
     * @param spawnAction 出怪动作（入参=敌人 id，由 GameController 建敌人并加入列表）
     * @return 本帧新出生的敌人数
     */
    public int update(long nowNanos, Consumer<String> spawnAction) {
        int born = 0;
        if (!spawning) return 0;
        while (spawned < orders.size()
                && nowNanos - waveStartNanos >= orders.get(spawned).offsetNanos) {
            spawnAction.accept(orders.get(spawned).type);
            spawned++;
            born++;
        }
        return born;
    }

    /** 是否仍在出怪期 */
    public boolean isSpawning() {
        return spawning && spawned < orders.size();
    }

    /** 重置（重新开始一局时由 GameController 调用） */
    public void reset() {
        orders.clear();
        waveStartNanos = -1;
        spawned = 0;
        spawning = false;
    }
}
