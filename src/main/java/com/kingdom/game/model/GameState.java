package com.kingdom.game.model;

import com.kingdom.game.config.GameConfig;

/**
 * GameState —— 金币 / 生命 / 波次等核心数值状态（纯数据，UI 刷新由 GameController 负责）。
 * 初值来自注入的 GameConfig（不写死）；缓存初值以支持 reset()。
 */
public class GameState {
    private final int initialGold;
    private final int initialLives;
    private final int totalWaves;

    private int gold;
    private int lives;
    private int wave = 0;

    public GameState(GameConfig config) {
        this.initialGold = config.getInitialGold();
        this.initialLives = config.getInitialLives();
        this.totalWaves = config.getTotalWaves();
        reset();
    }

    public boolean spendGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public void loseLives(int amount) {
        lives -= amount;
        if (lives < 0) lives = 0;
    }

    /** 波次 +1（开始一波时由 GameController 调用） */
    public void advanceWave() {
        wave++;
    }

    /** 重置回初值（重新开始一局） */
    public void reset() {
        gold = initialGold;
        lives = initialLives;
        wave = 0;
    }

    public boolean isGameOver() { return lives <= 0; }

    // Getter
    public int getGold() { return gold; }
    public int getLives() { return lives; }
    public int getWave() { return wave; }
    public int getTotalWaves() { return totalWaves; }
}
