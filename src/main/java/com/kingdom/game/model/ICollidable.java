package com.kingdom.game.model;

/**
 * ICollidable —— 可碰撞接口（model 层能力契约）。
 *
 * 由 LivingEntity 统一实现（半径 = max(width,height)/2）；
 * 具体实体（敌人/士兵等）如需特殊碰撞反应，覆写各自 handleEntityCollision 钩子。
 */
public interface ICollidable {

    /** 碰撞半径（用于圆心距离检测） */
    double getCollisionRadius();

    /**
     * 与另一个对象发生碰撞。
     *
     * @param other 发生碰撞的另一个游戏对象
     * @return true 表示本次碰撞被处理，false 表示忽略
     */
    boolean onCollision(GameObject other);

    /** 是否启用碰撞（死亡/眩晕等状态下应返回 false） */
    boolean isCollisionEnabled();
}
