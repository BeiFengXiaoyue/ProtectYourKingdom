package com.kingdom.game.model.ally;

import com.kingdom.game.model.Faction;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;

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

    /**
     * 近战出手：命中判定沿用基类，命中后交 {@link #onHit} 出表现。
     * 判定条件与基类 tryAttack 保持一致（眩晕/目标缺失/目标已死/冷却未到则不处理）。
     */
    @Override
    public void tryAttack(LivingEntity target) {
        if (isStunned || target == null || !target.isAlive() || attackCooldown > 0) return;
        super.tryAttack(target);
        onHit(target);
    }

    /**
     * 命中表现：与 Bomb.onHit 同一套 fxText 写法——在被击中的目标头顶（y-24）
     * 飘一条白色 "-伤害" 飘字。近战只做这一项，不加粒子/震屏等其它表现。
     */
    protected void onHit(LivingEntity target) {
        fxText.showFloatingText(target.getX(), target.getY() - 24, "-" + attackDamage, "WHITE");
    }

    /**
     * 动画帧叠加之后的额外绘制（默认空）。
     * 需要压在动画帧之上的标记（如精英兵种的金环/盔缨）由子类覆写本方法绘制；
     * 渲染层若有动画叠加，应在叠加之后调用本方法。
     */
    public void renderPostAnim(GraphicsContext gc) { /* 默认无 */ }

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

    /**
     * 每帧对外索敌：把 {@link #findTarget(List)} 的结果写入 target。
     * 由 GameController 每帧轮询调用 —— 使友方主动锁定视野内目标，而不仅靠碰撞接触。
     */
    public void pollTarget(List<Enemy> enemies) {
        this.target = findTarget(enemies);
    }

    public Enemy getTarget() { return target; }
}
