package com.kingdom.game.model.ally;

import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * Ally（友方抽象类）
 * 所有友方单位的基类，继承 LivingEntity，增加攻击逻辑（供士兵、未来弓箭手等复用）。
 */
public abstract class Ally extends LivingEntity {
    protected int attackDamage;
    protected int attackCooldown;
    protected int currentCooldown = 0;
    protected Enemy target;          // 当前攻击目标

    public Ally(double x, double y, int hp, double speed, int attackDamage, int attackCooldown) {
        super(x, y, hp, speed);
        this.attackDamage = attackDamage;
        this.attackCooldown = attackCooldown;
    }

    /** 寻找攻击目标（子类实现：最近敌人/血量最低等） */
    public abstract Enemy findTarget(List<Enemy> enemies);

    /** 执行攻击 */
    public abstract void attack(Enemy target);

    /** 尝试攻击（由 GameController 每帧调用） */
    public void tryAttack(List<Enemy> enemies) {
        if (isStunned || currentCooldown > 0) return;
        Enemy t = findTarget(enemies);
        if (t != null) {
            target = t;
            attack(t);
            currentCooldown = attackCooldown;
        }
    }

    @Override
    public void update() {
        super.update();  // 处理眩晕
        if (currentCooldown > 0) {
            currentCooldown -= 16;
            if (currentCooldown < 0) currentCooldown = 0;
        }
    }

    public int getAttackDamage() { return attackDamage; }
}
