package com.kingdom.game.model.enemy;

import com.kingdom.game.model.LivingEntity;

/**
 * Enemy（敌方抽象类）
 * 所有敌人的基类，继承 LivingEntity，增加路径跟随与赏金。
 */
public abstract class Enemy extends LivingEntity {
    protected int goldReward;
    protected int waypointIndex = 0;
    protected double[] pathX;
    protected double[] pathY;
    /**
     * 【集成补充，不改任何已有签名】
     * 敌人走完全程（onReachEnd 被触发）后置位，
     * 供 GameController 识别并扣除玩家生命值。
     */
    protected boolean reachedEnd = false;

    public Enemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed);
        this.goldReward = goldReward;
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
     * 到达终点：置走完标记后销毁。
     * 扣生命动作由 GameController 检测 hasReachedEnd() 后执行。
     */
    public void onReachEnd() {
        reachedEnd = true;
        destroy();
    }

    public void setPath(double[] pathX, double[] pathY) {
        this.pathX = pathX;
        this.pathY = pathY;
    }

    public boolean hasReachedEnd() { return reachedEnd; }
    public int getGoldReward() { return goldReward; }
}
