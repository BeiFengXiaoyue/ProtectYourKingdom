package com.kingdom.game.controller;

import com.kingdom.game.model.GameObject;

/**
 * ISelectionFx —— 视觉特效契约（选中高亮事项）。
 * 由 GameView（UI 主画布）实现；GameController 在塔选中/取消时调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface ISelectionFx {
    void showSelectionHighlight(GameObject target);

    void clearSelectionHighlight();
}
