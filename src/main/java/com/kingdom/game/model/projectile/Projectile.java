package com.kingdom.game.model.projectile;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.enemy.Enemy;

import java.util.List;

/**
 * Projectile（投射物抽象类）
 * 所有子弹/炮弹的基类，直接继承 GameObject，无 HP。
 *
 * 双构造模式（B 投射物开发者的开工前提）：
 * - 追踪模式：锁定具体 Enemy 实例，飞行中实时刷新目标坐标（箭矢 Arrow 用）；
 * - 定点模式：飞向固定坐标点（炮弹 Bomb 的落点用）。
 *
 * 契约：
 * - 本类 update() 负责"未命中则飞行"；
 * - 命中(hasHit=true)后，由 GameController 调用一次 {@link #onHit(List)} 并把它移出注册表。
 */
public abstract class Projectile extends GameObject {

    protected Enemy targetEnemy;    // 追踪目标（定点模式下为 null）
    protected double targetX, targetY;
    protected int damage;
    protected double speed;
    protected boolean hasHit = false;

    /** 追踪模式 */
    public Projectile(double x, double y, Enemy target, int damage, double speed) {
        super(x, y);
        this.targetEnemy = target;
        this.targetX = target != null ? target.getX() : x;
        this.targetY = target != null ? target.getY() : y;
        this.damage = damage;
        this.speed = speed;
    }

    /** 定点模式 */
    public Projectile(double x, double y, double targetX, double targetY, int damage, double speed) {
        super(x, y);
        this.targetEnemy = null;
        this.targetX = targetX;
        this.targetY = targetY;
        this.damage = damage;
        this.speed = speed;
    }

    /** 命中效果（单体扣血 / AOE 溅射），子类实现 */
    public abstract void onHit(List<Enemy> enemies);

    @Override
    public void update() {
        if (hasHit) return;
        fly();
    }

    /** 飞行移动（追踪模式实时锁定目标） */
    protected void fly() {
        if (targetEnemy != null && targetEnemy.isAlive()) {
            targetX = targetEnemy.getX();
            targetY = targetEnemy.getY();
        }
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

    // ===== Getter =====
    public boolean hasHit() { return hasHit; }
    public int getDamage() { return damage; }
    public boolean isTracking() { return targetEnemy != null; }
    public Enemy getTargetEnemy() { return targetEnemy; }
}
