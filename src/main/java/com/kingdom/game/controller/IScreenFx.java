package com.kingdom.game.controller;

/**
 * IScreenFx —— 视觉特效契约（屏幕级特效事项：闪屏/震屏/Boss 预警）。
 * 由 GameView（UI 主画布）实现；实体/控制器在相应时刻调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IScreenFx {
    void flashScreen(String color, int durationMs);

    void shakeScreen(int durationMs, int intensity);

    void showBossWarning();
}
