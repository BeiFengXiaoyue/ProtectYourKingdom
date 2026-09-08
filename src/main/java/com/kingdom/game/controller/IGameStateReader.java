package com.kingdom.game.controller;

import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;

import java.util.List;

/**
 * IGameStateReader（UI → 后端）
 * UI 组件主动查询当前游戏数据的只读契约。
 * 职责单一：只提供查询，不含任何修改操作。
 */
public interface IGameStateReader {

    int getCurrentGold();

    int getCurrentLives();

    int getCurrentWave();

    int getTotalWaves();

    List<Enemy> getEnemies();

    List<Tower> getTowers();

    List<Projectile> getProjectiles();

    List<Ally> getAllies();

    /** 可建造塔目录（建塔菜单数据源，含显示名与造价） */
    List<TowerSpec> getTowerSpecs();

    boolean isGameOver();

    boolean isVictory();
}
