package com.kingdom.game.model.enemy;

import com.kingdom.game.model.Faction;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.ally.Ally;

/**
 * Enemy（敌方抽象类）
 * 所有敌人的基类，继承 LivingEntity。
 *
 * 提供的基类行为（B 实体开发者可直接使用）：
 * - followPath()：沿路径逐点行走，走完全程触发 onReachEnd；
 * - 拦截追击：与友方发生碰撞(handleEntityCollision)后进入"追击+近战"，
 *   目标死亡/脱离后自动回路径继续走。
 */
public abstract class Enemy extends LivingEntity {

    protected int goldReward;
    protected int waypointIndex = 0;
    protected double[] pathX;
    protected double[] pathY;

    /** 已走完全程（GameController 据此扣生命并移除） */
    protected boolean reachedEnd = false;

    // ===== 拦截状态 =====
    protected LivingEntity engagedTarget = null;
    protected boolean isEngaged = false;

    public Enemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, Faction.ENEMY);
        this.goldReward = goldReward;
        this.attackDamage = 5;          // 默认近战攻击力（子类可覆写）
        this.maxAttackCooldown = 1000;  // 默认近战冷却 1s
    }

    // ===== 碰撞反应：遇友方进入拦截 =====
    @Override
    protected boolean handleEntityCollision(LivingEntity other) {
        if (other instanceof Ally) {
            if (!isEngaged) {
                isEngaged = true;
                engagedTarget = other;
            }
            tryAttack(other);
            return true;
        }
        return false;
    }

    // ===== 移动模板：拦截时追击，否则沿路径 =====
    @Override
    public void move() {
        if (isEngaged && engagedTarget != null && engagedTarget.isAlive()) {
            chaseAndAttack(engagedTarget);
        } else {
            isEngaged = false;
            engagedTarget = null;
            followPath();
        }
    }

    private void chaseAndAttack(LivingEntity target) {
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dist = Math.hypot(dx, dy);
        if (dist > 10) {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
        tryAttack(target);
    }

    /** 沿路径移动（供子类复用） */
    protected void followPath() {
        if (pathX == null || waypointIndex >= pathX.length) {
            onReachEnd();
            return;
        }
        double dx = pathX[waypointIndex] - x;
        double dy = pathY[waypointIndex] - y;
        double dist = Math.hypot(dx, dy);
        if (dist < speed * 0.016) {
            waypointIndex++;
        } else {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
    }

    /**
     * 到达终点：置 reachedEnd 并飘字提示。
     * 扣生命动作由 GameController 检测 hasReachedEnd() 后执行。
     */
    public void onReachEnd() {
        reachedEnd = true;
        fxText.showFloatingText(x, y - 24, "-1 ❤", "RED");
        destroy();
    }

    public void setPath(double[] pathX, double[] pathY) {
        this.pathX = pathX;
        this.pathY = pathY;
    }

    // ===== Getter =====
    public boolean hasReachedEnd() { return reachedEnd; }
    public int getGoldReward() { return goldReward; }
    public boolean isEngaged() { return isEngaged; }
    public LivingEntity getEngagedTarget() { return engagedTarget; }
}
