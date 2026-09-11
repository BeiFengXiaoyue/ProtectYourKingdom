package com.kingdom.game.model;

/**
 * TowerType —— 塔类型标识（轻量 id，不承载行为/造价）。
 *
 * 逐步开发约定：
 * - 当前含三族三级共 9 个值（箭塔 ARROW→ARROW_ELITE→ARROW_MASTER、炮塔 CANNON→…、兵营 BARRACK→…）；
 * - 新增塔时：
 *   ① 在此追加常量（防御塔负责人维护本文件）；
 *   ② 新建对应具体 Tower 子类；
 *   ③ 在装配处：1 级塔经 {@code GameController.registerTower(type, spec, factory)} 登记（同时进建塔菜单），
 *      升级级塔经 {@code GameController.registerUpgradeFactory(type, factory)} 登记（只进工厂表、不进菜单）。
 *
 * 显示名与造价统一见 {@link TowerSpec} / GameConfig，不写死在此枚举。
 */
public enum TowerType {
    ARROW,
    ARROW_ELITE,
    ARROW_MASTER,
    CANNON,
    CANNON_ELITE,
    CANNON_MASTER,
    BARRACK,
    BARRACK_ELITE,
    BARRACK_MASTER
}
