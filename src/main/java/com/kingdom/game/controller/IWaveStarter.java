package com.kingdom.game.controller;

/**
 * IWaveStarter（UI → 后端）
 * 开始波次，由"开始波次"按钮调用。
 */
public interface IWaveStarter {
    void startNextWave();

    boolean canStartNextWave();
}
