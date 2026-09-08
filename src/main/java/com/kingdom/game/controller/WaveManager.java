package com.kingdom.game.controller;

/**
 * WaveManager —— 只负责"按时间间隔出怪"的节奏控制。
 * 数值（每波数量/间隔）不写死，由 GameController 在 beginWave 时从 GameConfig 现取传入。
 */
public class WaveManager {

    private int enemyCount;
    private long spawnIntervalNanos;
    private long waveStartNanos = -1;
    private int spawned = 0;
    private boolean spawning = false;

    /** 开始一波（由 GameController 在 startNextWave 时调用） */
    public void beginWave(int enemyCount, long spawnIntervalMs) {
        this.enemyCount = enemyCount;
        this.spawnIntervalNanos = spawnIntervalMs * 1_000_000L;
        this.spawned = 0;
        this.waveStartNanos = System.nanoTime();
        this.spawning = true;
    }

    /**
     * 每帧推进：到期则调用 spawnAction 生成一只敌人。
     *
     * @param nowNanos    当前系统纳秒
     * @param spawnAction 出怪动作（创建敌人并加入 GameController 列表）
     * @return 本帧新出生的敌人数
     */
    public int update(long nowNanos, Runnable spawnAction) {
        int born = 0;
        if (!spawning) return 0;
        while (spawned < enemyCount && nowNanos - waveStartNanos >= spawned * spawnIntervalNanos) {
            spawnAction.run();
            spawned++;
            born++;
        }
        return born;
    }

    /** 是否仍在出怪期 */
    public boolean isSpawning() {
        return spawning && spawned < enemyCount;
    }
}
