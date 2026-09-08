package com.kingdom.game.model;

/**
 * LivingEntity（活体实体抽象类）
 * 所有"有生命、可受击"的单位（敌我双方）继承此类。
 */
public abstract class LivingEntity extends GameObject {
    protected int maxHp;
    protected int currentHp;
    protected double speed;          // 移动速度（所有活体都有）
    protected boolean isStunned = false;
    protected int stunTimer = 0;

    public LivingEntity(double x, double y, int maxHp, double speed) {
        super(x, y);
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.speed = speed;
    }

    public LivingEntity(double x, double y, int maxHp, double speed, double width, double height) {
        super(x, y, width, height);
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.speed = speed;
    }

    /** 受到伤害 */
    public void takeDamage(int damage) {
        if (currentHp <= 0) return;
        currentHp -= damage;
        if (currentHp <= 0) {
            currentHp = 0;
            onDeath();
        }
    }

    /** 死亡钩子，子类必须实现 */
    protected abstract void onDeath();

    /** 移动行为，子类实现（路径跟随/追击等） */
    public abstract void move();

    /** 眩晕 */
    public void stun(int milliseconds) {
        isStunned = true;
        stunTimer = milliseconds;
    }

    @Override
    public void update() {
        if (isStunned) {
            stunTimer -= 16;
            if (stunTimer <= 0) isStunned = false;
        }
        if (!isStunned) move();
    }

    // Getter
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public double getSpeed() { return speed; }
    public boolean isStunned() { return isStunned; }
    public boolean isAlive() { return currentHp > 0; }  // 辅助方法，非字段
}
