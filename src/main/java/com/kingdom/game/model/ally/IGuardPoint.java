package com.kingdom.game.model.ally;

/**
 * IGuardPoint —— 友方单位的「驻守点」能力契约（model 层能力接口，与 {@code ICollidable} 同级）。
 *
 * 背景：三档士兵（{@link Soldier} / {@link EliteSoldier} / {@link RoyalSoldier}）是 {@link Ally}
 * 的**并列子类、互不继承**（《友方士兵子系统说明》§2 的刻意取舍）。装配层需要在单位入战场时
 * 把「离兵营最近的路径点」注入进来，作为该单位的站岗与回防目标；若按具体兵种判定
 * （原为 {@code instanceof Soldier}），新增兵种必然漏注入——接口化后，任何实现本接口的
 * 友方单位都会被自动注入。
 *
 * 调用方：{@code GameController.addAlly}（全工程唯一注入点），在单位入 {@code allies}
 * 注册表**之前**注入。
 * 实现方：{@link Soldier}、{@link EliteSoldier}、{@link RoyalSoldier}。
 *
 * 约定：
 * - 无返回值、**无 default 方法**（对齐《接口契约与抽象类说明》「单一职责、无 default」口径）；
 * - 坐标系为画布像素，与 {@code GameObject.getX()/getY()} 同系；
 * - 允许重复注入，后注入覆盖前值；
 * - **未注入时的默认行为由实现方自持**（当前三档均取构造出生点），接口不强制。
 */
public interface IGuardPoint {

    /** 由装配层注入驻守点（= 离兵营最近的路径点） */
    void setGuardPoint(double x, double y);
}
