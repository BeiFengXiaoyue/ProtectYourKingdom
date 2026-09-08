package com.kingdom.game.controller;

import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.tower.Tower;

/**
 * ITowerBuilder（UI → 后端）
 * 建塔 / 升级 / 出售操作契约，由鼠标点击处理器与塔详情面板调用。
 * 职责单一：只负责塔的建造管理，状态查询走 IGameStateReader。
 */
public interface ITowerBuilder {

    /**
     * 尝试在 (x, y) 建造一座 type 塔。
     *
     * @return true=建造成功；false=失败（金币不足/越界/建在路径上/位置被占），
     *         失败原因会通过 IStatusObserver 消息提示
     */
    boolean placeTower(double x, double y, TowerType type);

    void upgradeTower(Tower tower);

    void sellTower(Tower tower);
}
