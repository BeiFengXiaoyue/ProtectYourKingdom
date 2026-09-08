package com.kingdom.game.controller;

/**
 * IFloatingTextFx —— 视觉特效契约（飘字事项）。
 * 由 GameView（UI 主画布）实现；实体/控制器在需要飘字处调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IFloatingTextFx {
    /**
     * @param x     世界坐标 x
     * @param y     世界坐标 y
     * @param text  文本内容（如 +10 🪙 / -1 ❤ / -15）
     * @param color 颜色名（RED / GOLD / GREEN / BLUE / WHITE 等）
     */
    void showFloatingText(double x, double y, String text, String color);
}
