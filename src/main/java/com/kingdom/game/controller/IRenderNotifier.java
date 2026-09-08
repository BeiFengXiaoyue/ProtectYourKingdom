package com.kingdom.game.controller;

/**
 * IRenderNotifier（后端 → UI）
 * 后端请求重绘整个游戏画布，由 GameView（主画布）实现。
 */
public interface IRenderNotifier {
    void requestRender();
}
