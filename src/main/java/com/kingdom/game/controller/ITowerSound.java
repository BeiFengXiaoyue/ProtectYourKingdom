package com.kingdom.game.controller;

import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.tower.Tower;

/**
 * ITowerSound —— 音效契约（建塔/升级/出售事项）。
 * 由音效管理器实现；GameController 在塔相关操作成功结算后调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface ITowerSound {
    void onTowerPlaced(TowerType type);

    void onTowerUpgraded(Tower tower);

    void onTowerSold(Tower tower);
}
