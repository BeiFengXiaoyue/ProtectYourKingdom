package com.kingdom.game.controller;

import com.kingdom.game.model.LivingEntity;

/**
 * IUnitSound —— 音效契约（单位出场事项）。
 * 由音效管理器实现；GameController 在士兵/单位被生产出场时调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IUnitSound {
    void onUnitSpawned(LivingEntity unit);
}
