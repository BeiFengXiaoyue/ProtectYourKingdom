package com.kingdom.game.controller;

/**
 * IEndSound —— 音效契约（胜负事项）。
 * 由音效管理器实现；GameController 检测到胜利/失败时调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IEndSound {
    void onVictory();

    void onGameOver();
}
