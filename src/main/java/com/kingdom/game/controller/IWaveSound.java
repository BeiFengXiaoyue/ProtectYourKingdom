package com.kingdom.game.controller;

/**
 * IWaveSound —— 音效契约（波次事项）。
 * 由音效管理器实现；GameController 在 startNextWave 成功开始新波时调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IWaveSound {
    void onWaveStarted(int waveNumber);
}
