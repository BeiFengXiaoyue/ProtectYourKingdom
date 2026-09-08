package com.kingdom.game.controller;

/**
 * IStatusObserver（后端 → UI）
 * 金币/生命/波次消息更新时回调，由 HUD（状态栏）实现。
 */
public interface IStatusObserver {
    void onGoldUpdated(int gold);

    void onLivesUpdated(int lives);

    void onWaveMessage(String message);
}
