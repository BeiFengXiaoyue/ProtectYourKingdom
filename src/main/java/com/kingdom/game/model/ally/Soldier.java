package com.kingdom.game.model.ally;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Soldier —— 近战士兵（兵营生产，拦截敌人）。
 *
 * 行为（继承 Enemy 同款帧步进 0.016）：
 * - 索敌：findTarget() 返回视野（VISION_RANGE）内最近存活敌人，供 GameController 每帧轮询；
 * - 追击：target 存活则就位至个人环形位（围殴同一敌人时各占一面）并近战（就位后才出手）；
 * - 回防：target 为 null/死亡时回到驻守点（未注入时=出生锚点）待命；
 * - 碰撞拦截由基类 Ally.handleEntityCollision() 完成（接触即锁定目标 + 近战反击）。
 * 数值（HP/速度/攻击力/冷却）由兵营（Barrack）生产时经构造函数传入，类内不写死。
 */
public class Soldier extends Ally {

    /** 索敌视野半径 px */
    private static final double VISION_RANGE = 90;

    /** 个人方位角（构造时随机固定）：站岗散布与围殴环形位共用，使多士兵自然错开 */
    private final double bearingAngle = Math.random() * 2 * Math.PI;

    /** 站岗散布半径（临时数值：暂定 18px，约一个身位；待定稿调整） */
    private static final double STAND_SPREAD = 18;

    /** 围殴环形半径（临时数值：暂定 8px = 敌人 10px 停步阈值 − 2px 就位容差，
     *  保证士兵就位后敌人停步条件即满足，消除"追环位"漂移；待定稿调整） */
    private static final double ATTACK_RING = 8;

    /** 回防到家判定距离 px */
    private static final double RETURN_EPSILON = 2;

    /** 出生锚点 */
    private final double homeX, homeY;

    /** 驻守点（回防目标；未注入时=出生点，行为与旧版一致） */
    private double guardX, guardY;

    /** 主动追击拴绳半径（临时数值：暂定 150px，防尾随敌人全场跑；待定稿调整） */
    private static final double LEASH_RANGE = 150;

    public Soldier(double x, double y, int hp, double speed,
                   int attackDamage, int attackCooldown) {
        super(x, y, hp, speed, attackDamage, attackCooldown);
        setWidth(18);
        setHeight(18);
        this.homeX = x;
        this.homeY = y;
        this.guardX = x;
        this.guardY = y;
    }

    /** 由装配层（GameController.addAlly）注入驻守点（=离兵营最近的路径点） */
    public void setGuardPoint(double x, double y) {
        this.guardX = x;
        this.guardY = y;
    }

    /** 索敌：返回视野内最近的存活敌人；视野内无目标返回 null */
    @Override
    public Enemy findTarget(List<Enemy> enemies) {
        Enemy nearest = null;
        double minDist = VISION_RANGE;
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            double dist = Math.hypot(e.getX() - x, e.getY() - y);
            if (dist <= minDist) {
                minDist = dist;
                nearest = e;
            }
        }
        return nearest;
    }

    /**
     * 移动：锁定目标则就位个人环形位并近战（就位才出手）；目标缺失/死亡则清空并回防个人岗位待命。
     * （锁定的目标由碰撞拦截或 GameController 每帧轮询 findTarget 写入）
     */
    @Override
    public void move() {
        if (target == null || !target.isAlive()) {
            target = null;
            returnHome();
            return;
        }
        if (Math.hypot(guardX - x, guardY - y) > LEASH_RANGE) {
            target = null;   // 离驻守点过远：放弃追击回防（防尾随敌人脱离防区）
            returnHome();
            return;
        }
        // 个人环形位：目标中心 + 环形半径×个人方位角（围殴同一敌人时各占一面不叠点）
        double rx = target.getX() + Math.cos(bearingAngle) * ATTACK_RING;
        double ry = target.getY() + Math.sin(bearingAngle) * ATTACK_RING;
        double dx = rx - x;
        double dy = ry - y;
        double dist = Math.hypot(dx, dy);
        if (dist > RETURN_EPSILON) {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
        // 就位才出手：与敌人中心足够近才允许近战（消灭跑动中隔空命中）
        if (Math.hypot(target.getX() - x, target.getY() - y) <= ATTACK_RING + RETURN_EPSILON) {
            tryAttack(target);
        }
    }

    /** 回防：走回个人岗位（驻守点 + 站岗散布×个人方位角；未注入驻守点时=出生锚点附近）；到岗则原地待命 */
    private void returnHome() {
        double sx = guardX + Math.cos(bearingAngle) * STAND_SPREAD;
        double sy = guardY + Math.sin(bearingAngle) * STAND_SPREAD;
        double dx = sx - x;
        double dy = sy - y;
        double dist = Math.hypot(dx, dy);
        if (dist > RETURN_EPSILON) {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
    }

    @Override
    protected void onDeath() {
        // 死亡表现：蓝色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 移除与结算由 GameController 完成，实体不做列表/金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#4499ff", 10);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为蓝色圆形
        if (!drawSprite(gc, AssetKey.SOLDIER)) {
            double r = width / 2;
            gc.setFill(Color.web("#3498db"));
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#1f618d"));
            gc.setLineWidth(1.5);
            gc.strokeOval(x - r, y - r, width, height);
        }

        // 头顶血条
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 8, width, 4);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 8, width * ratio, 4);
    }
}
