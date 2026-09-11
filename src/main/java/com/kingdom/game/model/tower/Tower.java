package com.kingdom.game.model.tower;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.TowerSpec;
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
 *
 * 数值通道（PM 要求：参数不用 final、改由构造注入）：
 * 塔自身**不再持有任何数值常量**，数值统一由 {@link TowerParams} 在构造期注入
 * （默认值集中在 {@code TowerParamsDefaults}）；接线人员改数值只改那一处。
 */
public abstract class Tower extends GameObject {

    protected double attackRange;
    protected int attackCooldown;
    protected int currentCooldown = 0;
    protected int baseAttackDamage;
    protected int totalCost = 0;
    protected boolean isStunned = false;
    protected int stunTimer = 0;

    /** 注入的数值参数（构造期由子类写入；等级、升级链信息均从它取） */
    protected TowerParams params;

    /** 投射物出口（由 GameController 于 placeTower/upgradeTower 注入；未注入时发射动作丢弃、不报错） */
    protected Consumer<Projectile> projectileSink = p -> { };

    public Tower(double x, double y) {
        super(x, y);
    }

    /**
     * 由子类构造在注入数值后调用：把 {@link TowerParams} 的公共战斗值写入本塔字段。
     * （兵营专属参数与各族投射物速度各自处理。）
     */
    protected void applyParams(TowerParams p) {
        this.params = p;
        this.attackRange = p.getRange();
        this.attackCooldown = p.getCooldownMs();
        this.baseAttackDamage = p.getDamage();
        this.totalCost = p.getTotalCost();
    }

    /** 由注入的 params 构造"下一级目录条目"；满级（nextLevelType == null）返回 null */
    protected TowerSpec nextSpecFrom(TowerParams p) {
        if (p.getNextLevelType() == null) return null;
        return new TowerSpec(p.getNextLevelType(), p.getNextLevelName(), p.getUpgradeCost());
    }

    /** 本塔等级（1/2/3）——等级随数值注入，不再由类常量声明 */
    public int getLevel() { return params == null ? 1 : params.getLevel(); }

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
