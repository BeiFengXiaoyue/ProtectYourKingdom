package com.kingdom.game.controller;

import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * IGameStateReader（UI → 后端）
 * UI 组件主动查询当前游戏数据（金币/生命/波次/在场敌人等）。
 */
public interface IGameStateReader {
    int getCurrentGold();

    int getCurrentLives();

    int getCurrentWave();

    int getTotalWaves();

    List<Enemy> getEnemies();
}
