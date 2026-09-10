package com.kingdom.game.util.anim;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.enemy.Enemy;

/**
 * UnitPoseResolver —— 单位行为模式解析器（纯规则，无状态）。
 *
 * 设计意图（见 docs/单位行为动画-渲染层设计.md §2）：
 * 单位类**自身没有**“当前行为模式”字段，动画层只做只读观察，
 * 用公开状态（坐标位移 / 眩晕 / 攻击目标）每帧推导出模式名。
 *
 * 判定优先级：眩晕 → idle &gt; 贴身近战 → attack &gt; 本帧有位移 → walk &gt; 兜底 idle。
 * 非活体（防御塔等）恒为 idle。模式名取小写下划线，与 AnimTable 的 modes 键对应。
 */
public final class UnitPoseResolver {

    /** 待命模式名 */
    public static final String MODE_IDLE = "idle";
    /** 行走模式名 */
    public static final String MODE_WALK = "walk";
    /** 攻击模式名 */
    public static final String MODE_ATTACK = "attack";

    /** 贴身近战距离（与 Enemy.chaseAndAttack / Soldier.MELEE_RANGE 一致） */
    private static final double MELEE_DIST = 10;

    private UnitPoseResolver() {
    }

    /**
     * 解析某单位本帧应播放的模式。
     *
     * @param unit           单位（任意 GameObject；非活体恒 idle）
     * @param movedThisFrame 与上一帧相比是否有位移（由 UnitAnimator 按实例记录）
     * @return 模式名（idle / walk / attack …）
     */
    public static String resolve(GameObject unit, boolean movedThisFrame) {
        if (!(unit instanceof LivingEntity)) return MODE_IDLE;
        LivingEntity self = (LivingEntity) unit;

        if (self.isStunned()) return MODE_IDLE;          // 眩晕：停帧待命
        if (isMeleeAttacking(unit, self)) return MODE_ATTACK;
        if (movedThisFrame) return MODE_WALK;
        return MODE_IDLE;
    }

    /** 是否处于“贴身近战”（敌人看拦截目标，友方看当前攻击目标） */
    private static boolean isMeleeAttacking(GameObject unit, LivingEntity self) {
        LivingEntity target = null;
        if (unit instanceof Enemy) {
            Enemy e = (Enemy) unit;
            if (e.isEngaged()) target = e.getEngagedTarget();
        } else if (unit instanceof Ally) {
            target = ((Ally) unit).getTarget();
        }
        if (target == null || !target.isAlive()) return false;
        double dist = Math.hypot(target.getX() - self.getX(), target.getY() - self.getY());
        return dist <= MELEE_DIST;
    }
}
