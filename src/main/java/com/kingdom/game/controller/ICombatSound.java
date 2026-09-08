package com.kingdom.game.controller;

import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.projectile.Projectile;

/**
 * ICombatSound —— 音效契约（战斗事项：射击/命中/近战/死亡/Boss 震地）。
 * 由音效管理器实现；触发点分布在实体战斗行为与 GameController 结算处。
 * 单一职责、无 default 方法：实现方必须覆盖全部方法。
 */
public interface ICombatSound {
    void onProjectileFired(Projectile projectile);

    void onProjectileHit(Projectile projectile, LivingEntity target);

    /** 近战攻击（敌/友均可，由 attacker.getFaction() 区分音效） */
    void onUnitAttack(LivingEntity attacker, LivingEntity target);

    void onUnitDied(LivingEntity unit);

    void onBossStomp();
}
