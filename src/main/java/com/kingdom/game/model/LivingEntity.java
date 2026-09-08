package com.kingdom.game.model;

/**
 * LivingEntity（活体实体抽象类）
 * 所有"有生命、可受击"的单位（敌我双方）继承此类。
 *
 * 上提的公共能力（B 实体开发者的开工前提）：
 * - 阵营 Faction：可运行期经 setFaction 修改；
 * - 通用近战三件套 attackDamage / attackCooldown / maxAttackCooldown + tryAttack；
 * - ICollidable：同阵营不触发碰撞反应，跨阵营交给 handleEntityCollision 钩子。
 */
public abstract class LivingEntity extends GameObject implements ICollidable {

    // ===== 生命 =====
    protected int maxHp;
    protected int currentHp;
    protected double speed;

    // ===== 阵营（可运行期修改）=====
    protected Faction faction;

    // ===== 通用近战属性 =====
    protected int attackDamage;
    protected int attackCooldown;
    protected int maxAttackCooldown;

    // ===== 状态 =====
    protected boolean isStunned = false;
    protected int stunTimer = 0;

    public LivingEntity(double x, double y, int maxHp, double speed, Faction faction) {
        super(x, y);
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.speed = speed;
        this.faction = faction;
    }

    public LivingEntity(double x, double y, int maxHp, double speed, Faction faction,
                        double width, double height) {
        super(x, y, width, height);
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.speed = speed;
        this.faction = faction;
    }

    // ===== 受击与死亡 =====
    /** 受到伤害 */
    public void takeDamage(int damage) {
        if (currentHp <= 0) return;
        currentHp -= damage;
        if (currentHp <= 0) {
            currentHp = 0;
            combatSound.onUnitDied(this);
            onDeath();
        }
    }

    /** 死亡钩子，子类必须实现（死亡表现/后续结算归 GameController） */
    protected abstract void onDeath();

    // ===== 攻击 =====
    /**
     * 近战攻击模板：眩晕/冷却/目标存活任一不满足则跳过。
     * 成功时触发 combatSound.onUnitAttack。
     */
    public void tryAttack(LivingEntity target) {
        if (isStunned || target == null || !target.isAlive() || attackCooldown > 0) return;
        target.takeDamage(attackDamage);
        attackCooldown = maxAttackCooldown;
        combatSound.onUnitAttack(this, target);
    }

    // ===== ICollidable =====
    @Override
    public double getCollisionRadius() {
        return Math.max(width, height) / 2.0;
    }

    @Override
    public boolean isCollisionEnabled() {
        return isAlive() && !isStunned;
    }

    @Override
    public boolean onCollision(GameObject other) {
        if (!(other instanceof LivingEntity)) return false;
        LivingEntity target = (LivingEntity) other;
        if (!target.isCollisionEnabled() || !isCollisionEnabled()) return false;
        if (faction == target.faction) return false;   // 同阵营不触发碰撞反应
        return handleEntityCollision(target);
    }

    /** 跨阵营碰撞反应钩子，子类覆写（默认忽略） */
    protected boolean handleEntityCollision(LivingEntity other) {
        return false;
    }

    // ===== 移动 / 眩晕 / 更新 =====
    /** 移动行为（路径跟随/追击），子类实现 */
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
        if (attackCooldown > 0) {
            attackCooldown -= 16;
            if (attackCooldown < 0) attackCooldown = 0;
        }
        if (!isStunned && isAlive()) move();
    }

    // ===== Getter / Setter =====
    public int getCurrentHp() { return currentHp; }
    public int getMaxHp() { return maxHp; }
    public double getSpeed() { return speed; }
    public boolean isStunned() { return isStunned; }
    public boolean isAlive() { return currentHp > 0; }  // 辅助方法，非字段
    public Faction getFaction() { return faction; }

    /** 修改阵营（如魅惑/策反等特殊机制） */
    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public int getAttackDamage() { return attackDamage; }
    public int getMaxAttackCooldown() { return maxAttackCooldown; }
}
