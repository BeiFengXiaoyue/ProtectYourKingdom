package com.kingdom.game.model.tower;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Projectile;

import java.util.List;
import java.util.function.Consumer;

/**
 * Tower（防御塔抽象类）
 * 所有塔的基类，直接继承 GameObject，无 HP。
 *
 * 世界能力通道：塔不直接操作 GameController，而是通过 {@link #projectileSink}
 * 把"要发射的投射物"交给出口。GameController 在放置塔时用
 * {@link #setProjectileSink(Consumer)} 把出口接上"projectiles 注册表"。
 */
public abstract class Tower extends GameObject {

    protected double attackRange;
    protected int attackCooldown;
    protected int currentCooldown = 0;
    protected int baseAttackDamage;
    protected int totalCost = 0;
    protected boolean isStunned = false;
    protected int stunTimer = 0;

    /** 投射物出口（由 GameController 于 placeTower/upgradeTower 注入；未注入时发射动作丢弃、不报错） */
    protected Consumer<Projectile> projectileSink = p -> { };

    public Tower(double x, double y) {
        super(x, y);
    }

    /** 由 GameController 注入投射物出口 */
    public void setProjectileSink(Consumer<Projectile> sink) {
        if (sink != null) this.projectileSink = sink;
    }

    /** 把生成好的投射物交给出口（子类 attack 中调用） */
    protected void fire(Projectile projectile) {
        projectileSink.accept(projectile);
    }

    public abstract Enemy findTarget(List<Enemy> enemies);

    public abstract void attack(Enemy target);

    @Override
    public void update() {
        if (isStunned) {
            stunTimer -= 16;
            if (stunTimer <= 0) isStunned = false;
        }
        if (currentCooldown > 0) {
            currentCooldown -= 16;
            if (currentCooldown < 0) currentCooldown = 0;
        }
    }

    /** 索敌并开火（由 GameController 每帧调用） */
    public void tryAttack(List<Enemy> enemies) {
        if (isStunned || currentCooldown > 0) return;
        Enemy target = findTarget(enemies);
        if (target != null) {
            attack(target);
            currentCooldown = attackCooldown;
        }
    }

    /** 出售返还 50% 投入（返回退款额；列表移除由 GameController 完成） */
    public int sell() {
        destroy();
        return totalCost / 2;
    }

    public void stun(int milliseconds) {
        isStunned = true;
        stunTimer = milliseconds;
    }

    // ===== Getter =====
    public double getAttackRange() { return attackRange; }
    public int getBaseAttackDamage() { return baseAttackDamage; }
    public boolean isStunned() { return isStunned; }
    public int getTotalCost() { return totalCost; }
}
