package com.kingdom.game.model.tower;

/**
 * IHealAura —— 治疗光环契约（兵营 3 级特殊能力）。
 *
 * 策划文档：3 级兵营周围半径 {@link #getAuraRadius()} 内的士兵，每秒回复
 * {@link #getHealPerSecond()} 点生命。
 *
 * ⚠ 本次仅"留接口"：实际回血需要士兵/活体侧支持治疗（`LivingEntity`，B 名下），
 * 待其就绪后读取本接口参数，由兵营每帧对范围内的己方士兵施放。当前实现方（MasterBarrack）只暴露参数，不产生效果。
 */
public interface IHealAura {

    /** 每秒回血量 */
    int getHealPerSecond();

    /** 光环半径（px） */
    double getAuraRadius();
}
