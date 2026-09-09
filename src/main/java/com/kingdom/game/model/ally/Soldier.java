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
 * - 追击：target 存活则贴身至 MELEE_RANGE 并近战（tryAttack 内部按冷却节拍）；
 * - 回防：target 为 null/死亡时回到出生锚点 (homeX, homeY) 待命；
 * - 碰撞拦截由基类 Ally.handleEntityCollision() 完成（接触即锁定目标 + 近战反击）。
 * 数值（HP/速度/攻击力/冷却）由兵营（Barrack）生产时经构造函数传入，类内不写死。
 */
public class Soldier extends Ally {

    /** 索敌视野半径 px */
    private static final double VISION_RANGE = 90;

    /** 贴身近战距离 px（与 Enemy.chaseAndAttack 的贴身阈值一致） */
    private static final double MELEE_RANGE = 10;

    /** 回防到家判定距离 px */
    private static final double RETURN_EPSILON = 2;

    /** 出生锚点（回防目标） */
    private final double homeX, homeY;

    public Soldier(double x, double y, int hp, double speed,
                   int attackDamage, int attackCooldown) {
        super(x, y, hp, speed, attackDamage, attackCooldown);
        setWidth(60);   // 与普通敌人（60×60）同尺寸
        setHeight(60);
        this.homeX = x;
        this.homeY = y;
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
     * 移动：锁定目标则追击近战；目标缺失/死亡则清空并回防出生点待命。
     * （锁定的目标由碰撞拦截或 GameController 每帧轮询 findTarget 写入）
     */
    @Override
    public void move() {
        if (target == null || !target.isAlive()) {
            target = null;
            returnHome();
            return;
        }
        double dx = target.getX() - x;
        double dy = target.getY() - y;
        double dist = Math.hypot(dx, dy);
        if (dist > MELEE_RANGE) {
            x += (dx / dist) * speed * 0.016;
            y += (dy / dist) * speed * 0.016;
        }
        tryAttack(target);
    }

    /** 回防：离开出生锚点则走回；已在家则原地待命 */
    private void returnHome() {
        double dx = homeX - x;
        double dy = homeY - y;
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
