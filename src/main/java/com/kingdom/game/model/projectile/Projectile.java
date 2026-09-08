package com.kingdom.game.model.projectile;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * Projectile（投射物抽象类）
 * 所有子弹/炮弹的基类，直接继承 GameObject，无 HP。
 */
public abstract class Projectile extends GameObject {
    protected double targetX, targetY;
    protected int damage;
    protected double speed;
    protected boolean hasHit = false;

    public Projectile(double x, double y, double tx, double ty, int damage, double speed) {
        super(x, y);
        this.targetX = tx;
        this.targetY = ty;
        this.damage = damage;
        this.speed = speed;
    }

    /** 命中效果（单体扣血 / AOE） */
    public abstract void onHit(List<Enemy> enemies);

    /** 飞行移动（子类可复用） */
    protected void fly() {
        double dx = targetX - x;
        double dy = targetY - y;
        double dist = Math.hypot(dx, dy);
        if (dist < speed * 0.016) {
            x = targetX;
            y = targetY;
            hasHit = true;
        } else {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
    }

    public boolean hasHit() { return hasHit; }
    public int getDamage() { return damage; }
}
