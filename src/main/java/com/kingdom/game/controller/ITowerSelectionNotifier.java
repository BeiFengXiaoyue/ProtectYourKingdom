package com.kingdom.game.controller;

import com.kingdom.game.model.tower.Tower;

/**
 * ITowerSelectionNotifier（后端 → UI）
 * 塔被选中/取消选中时通知详情面板，由 TowerDetailPanel 实现。
 */
public interface ITowerSelectionNotifier {
    void onTowerSelected(Tower tower);

    void onTowerDeselected();
}
