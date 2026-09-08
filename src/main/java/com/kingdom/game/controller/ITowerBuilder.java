package com.kingdom.game.controller;

import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.tower.Tower;

/**
 * ITowerBuilder（UI → 后端）
 * 建塔 / 升级 / 出售，由鼠标点击处理器与塔详情面板调用。
 */
public interface ITowerBuilder {
    void placeTower(double x, double y, TowerType type);

    void upgradeTower(Tower tower);

    void sellTower(Tower tower);
}
