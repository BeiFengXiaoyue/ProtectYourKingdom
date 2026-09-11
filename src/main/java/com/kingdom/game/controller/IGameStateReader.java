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

    /** 波间倒计时剩余毫秒；无倒计时（第 1 波前 / 波次进行中 / 已结束）返回 0 */
    long getNextWaveCountdownMs();

    /** 此刻提前开始下一波的金币奖励预览（按剩余比例×上限计算）；不可提前返回 0 */
    int getEarlyStartBonus();

    List<Enemy> getEnemies();

    List<Tower> getTowers();

    List<Projectile> getProjectiles();

    List<Ally> getAllies();

    /** 可建造塔目录（建塔菜单数据源，含显示名与造价） */
    List<TowerSpec> getTowerSpecs();

    boolean isGameOver();

    boolean isVictory();

    // ===== 关卡查询（只读：供 UI 渲染关卡列表 / 上下一关按钮可用性）=====

    /** 全部关卡，顺序即关卡顺序（maps/index.json 数组顺序） */
    List<LevelInfo> getLevels();

    /** 关卡总数 */
    int getLevelCount();

    /** 当前关卡信息；当前 key 未登记时返回 null */
    LevelInfo getCurrentLevel();

    /** 当前关卡序号；未匹配到返回 -1 */
    int getCurrentLevelIndex();

    /** 当前关卡 key（= 当前地图 key） */
    String getCurrentLevelKey();

    /** 是否存在下一关 */
    boolean hasNextLevel();

    /** 是否存在上一关 */
    boolean hasPrevLevel();
}
