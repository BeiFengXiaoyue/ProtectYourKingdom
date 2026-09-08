package com.kingdom.game.model;

import com.kingdom.game.config.GameConfig;

/**
 * GameState —— 金币 / 生命 / 波次等核心数值状态（纯数据，UI 刷新由 GameController 负责）。
 * 初值来自注入的 GameConfig（不写死）。
 */
public class GameState {
    private int gold;
    private int lives;
    private int wave = 0;
    private final int totalWaves;

    public GameState(GameConfig config) {
        this.gold = config.getInitialGold();
        this.lives = config.getInitialLives();
        this.totalWaves = config.getTotalWaves();
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

    public boolean isGameOver() { return lives <= 0; }

    // Getter
    public int getGold() { return gold; }
    public int getLives() { return lives; }
    public int getWave() { return wave; }
    public int getTotalWaves() { return totalWaves; }
}
