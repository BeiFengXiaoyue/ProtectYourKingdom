package com.kingdom.game.model.tower;

/**
 * IChainShot —— 连锁箭契约（箭塔 3 级特殊能力）。
 *
 * 策划文档：3 级箭塔主目标命中后，溅射附近 {@link #getChainTargets()} 个敌人，
 * 各受主目标伤害的 {@link #getChainDamageRatio()} 倍。
 *
 * ⚠ 本次仅"留接口"：实际连锁逻辑在投射物侧（`Arrow.onHit`，B 名下），
 * 待投射物支持连锁后读取本接口参数接入。当前实现方（MasterArrowTower）只暴露参数，不产生效果。
 */
public interface IChainShot {

    /** 连锁目标数（主目标附近额外受溅射的敌人数） */
    int getChainTargets();

    /** 连锁伤害比例（相对主目标伤害，0~1） */
    double getChainDamageRatio();
}
