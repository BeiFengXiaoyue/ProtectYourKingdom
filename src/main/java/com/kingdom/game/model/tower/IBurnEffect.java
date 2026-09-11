package com.kingdom.game.model.tower;

/**
 * IBurnEffect —— 燃烧契约（炮塔 3 级特殊能力）。
 *
 * 策划文档：3 级炮塔命中后，敌人每秒受 {@link #getBurnDamagePerSecond()} 点伤害，
 * 持续 {@link #getBurnDurationMillis()} 毫秒。
 *
 * ⚠ 本次仅"留接口"：实际 DoT 需要敌人侧支持持续伤害（`Enemy`/`LivingEntity`，B 名下），
 * 待其就绪后读取本接口参数接入。当前实现方（MasterCannonTower）只暴露参数，不产生效果。
 */
public interface IBurnEffect {

    /** 燃烧每秒伤害 */
    int getBurnDamagePerSecond();

    /** 燃烧持续时间（毫秒） */
    int getBurnDurationMillis();
}
