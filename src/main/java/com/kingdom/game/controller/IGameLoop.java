package com.kingdom.game.controller;

/**
 * IGameLoop（UI → 后端）
 * 每帧更新所有游戏逻辑，由 AnimationTimer 循环驱动。
 */
public interface IGameLoop {
    void update(long nanoTime);

    /**
     * 暂停世界：冻结两个"绝对时间"基准（波间倒计时、出怪锚点）。幂等。
     * 注意：UI 层停 AnimationTimer 只让 update 不再被调用，时间基准仍按真实时间流逝，
     * 故暂停/恢复必须成对调用本接口，否则恢复后倒计时与出怪节奏会发生跳变。
     */
    void pause();

    /** 恢复世界：把本次暂停时长从两个绝对时间基准上整体后移。幂等。 */
    void resume();

    /** 是否处于暂停态。 */
    boolean isPaused();
}
