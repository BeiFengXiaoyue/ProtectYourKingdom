package com.kingdom.game.model.ally;

import com.kingdom.game.model.Faction;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * Ally（友方抽象类）
 * 所有友方单位的基类，继承 LivingEntity。
 * 公共攻击/冷却字段与方法已上提到 LivingEntity；本类只补友方语义：
 * 索敌抽象 + 对敌人近战碰撞反应。
 */
public abstract class Ally extends LivingEntity {

    /** 当前攻击目标（由索敌/移动逻辑维护） */
    protected Enemy target;

    public Ally(double x, double y, int hp, double speed, int attackDamage, int attackCooldown) {
        super(x, y, hp, speed, Faction.ALLY);
        this.attackDamage = attackDamage;
        this.maxAttackCooldown = attackCooldown;
        this.attackCooldown = 0;
    }

    /** 索敌策略（最近敌人/血量最低等），子类实现 */
    public abstract Enemy findTarget(List<Enemy> enemies);

    /**
     * 控制层每帧喂目标（契约帧序预留的"友方 AI"位）：
     * 仅当无目标或目标已死时才接新目标，防逐帧换目标抖动；null 无操作。
     * 碰撞直接锁定的目标（handleEntityCollision）优先级不变。
     */
    public void engage(Enemy e) {
        if (e == null) return;
        if (target == null || !target.isAlive()) target = e;
    }

    /** 碰撞反应：碰到敌人则近战攻击 */
    @Override
    protected boolean handleEntityCollision(LivingEntity other) {
        if (other instanceof Enemy) {
            target = (Enemy) other;
            tryAttack(other);
            return true;
        }
        return false;
    }

    public Enemy getTarget() { return target; }
}
