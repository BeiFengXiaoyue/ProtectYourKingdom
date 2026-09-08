package com.kingdom.game.controller;

/**
 * IParticleFx —— 视觉特效契约（粒子/爆炸事项）。
 * 由 GameView（UI 主画布）实现；实体/控制器在爆炸、击中特效处调用。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface IParticleFx {
    void spawnExplosionParticles(double x, double y, String color, int count);
}
