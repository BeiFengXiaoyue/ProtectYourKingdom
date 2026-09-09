package com.kingdom.game.model;

/**
 * TowerType —— 塔类型标识（轻量 id，不承载行为/造价）。
 *
 * 逐步开发约定：
 * - 当前只实现 ARROW（箭塔）；
 * - 新增塔时（如炮塔/兵营）：
 *   ① 在此追加常量（防御塔负责人维护本文件）；
 *   ② 新建对应具体 Tower 子类；
 *   ③ 在装配处 config.addTowerSpec(new TowerSpec(...)) 并提供
 *      GameController.registerTowerFactory(type, factory)。
 *
 * 显示名与造价统一见 {@link TowerSpec} / GameConfig，不写死在此枚举。
 */
public enum TowerType {
    ARROW,
    CANNON,
    BARRACK
}
