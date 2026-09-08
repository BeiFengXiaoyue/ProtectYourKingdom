package com.kingdom.game.controller;

/**
 * IGameLoop（UI → 后端）
 * 每帧更新所有游戏逻辑，由 AnimationTimer 循环驱动。
 */
public interface IGameLoop {
    void update(long nanoTime);
}
