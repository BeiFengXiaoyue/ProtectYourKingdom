package com.kingdom.game.model.tower;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * Tower（防御塔抽象类）
 * 所有塔的基类，直接继承 GameObject，无 HP。
 */
public abstract class Tower extends GameObject {
    protected double attackRange;
    protected int attackCooldown;
    protected int currentCooldown = 0;
    protected int level = 1;
    protected int baseAttackDamage;
    protected int upgradeCostBase;
    protected int totalCost = 0;
    protected boolean isStunned = false;
    protected int stunTimer = 0;

    public Tower(double x, double y) {
        super(x, y);
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

    public void tryAttack(List<Enemy> enemies) {
        if (isStunned || currentCooldown > 0) return;
        Enemy target = findTarget(enemies);
        if (target != null) {
            attack(target);
            currentCooldown = attackCooldown;
        }
    }

    public void upgrade() {
        level++;
        baseAttackDamage = (int) (baseAttackDamage * 1.3);
        upgradeCostBase = (int) (upgradeCostBase * 1.2);
    }

    public int sell() {
        destroy();
        return totalCost / 2;
    }

    public void stun(int milliseconds) {
        isStunned = true;
        stunTimer = milliseconds;
    }

    // Getter
    public double getAttackRange() { return attackRange; }
    public int getLevel() { return level; }
    public int getUpgradeCost() { return upgradeCostBase * level; }
    public int getBaseAttackDamage() { return baseAttackDamage; }
    public boolean isStunned() { return isStunned; }
}
