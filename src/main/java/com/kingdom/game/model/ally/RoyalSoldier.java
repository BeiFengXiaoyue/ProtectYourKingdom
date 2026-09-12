package com.kingdom.game.model.ally;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * RoyalSoldier —— 皇家卫士（士兵 **3 级**，当前最高档）。
 *
 * 分级方式：1 级 {@link Soldier}、2 级 {@link EliteSoldier}、3 级本类是三个**并列**的具体单位，
 * 都直接继承 {@link Ally}，**互不继承**——本类不继承 2 级，2 级的改动手工不会波及本类，
 * 反之亦然；三级各自完整实现索敌/追击/回防/死亡/渲染。
 *
 * 行为（与前两级同一套，速率按 3 级档位）：
 * - 索敌：findTarget() 返回视野（VISION_RANGE）内最近存活敌人，供 GameController 每帧轮询；
 * - 追击：target 存活则走向个人环形位（目标中心 + 个人方位角×ATTACK_RING），**就位后才近战**；
 *   离驻守点超过 LEASH_RANGE 则放弃追击回防（防尾随敌人脱离防区）；
 * - 回防：target 为 null/死亡时回到驻守点（未注入时=出生点）+ 个人站岗散布待命；
 * - 碰撞拦截由基类 Ally.handleEntityCollision() 完成（接触即锁定目标 + 近战反击）。
 *
 * 3 级档默认数值：HP 150 / 速度 72 / 攻击 20 / 冷却 620ms / 视野 150px / 体型 22×22
 * （1 级 70 / 60 / 10 / 800ms / 90px / 18×18；2 级 100 / 65 / 14 / 700ms / 120px / 20×20）。
 * 三级数值依次提高，符合《建筑与怪物机制策划》的兵营升级思路（更高等级 = 更耐打、更快、更痛）。
 *
 * 数值归属：与 Soldier 一致，生产方（兵营/装配层）可经全参构造注入；无参档案构造只是
 * “直接实例化”时的兜底，不是唯一数值源。
 */
public class RoyalSoldier extends Ally implements IGuardPoint {

    // ===== 3 级默认档案（2 级 → 3 级：HP 100→150、速度 65→72、攻击 14→20、冷却 700→620）=====
    /** 3 级默认生命值 */
    public static final int DEFAULT_HP = 150;
    /** 3 级默认移动速度 px/s */
    public static final double DEFAULT_SPEED = 72;
    /** 3 级默认攻击力 */
    public static final int DEFAULT_ATTACK = 20;
    /** 3 级默认攻击冷却 ms */
    public static final int DEFAULT_COOLDOWN = 620;

    /** 索敌视野半径 px（2 级 120 → 3 级 150） */
    private static final double VISION_RANGE = 150;

    /** 个人方位角（构造时随机固定）：站岗散布与围殴环形位共用，使多士兵自然错开 */
    private final double bearingAngle = Math.random() * 2 * Math.PI;

    /** 站岗散布半径 px（≈3/4 体型：两个反向站位的间距 = 2×散布 = 60px > 体型 40px，最坏情况不重叠） */
    private static final double STAND_SPREAD = 30;

    /** 围殴环形半径 px（= 敌人 10px 停步阈值 − 2px 就位容差，与 1 级兵同值；待定稿调整） */
    private static final double ATTACK_RING = 8;

    /** 回防到家判定距离 px */
    private static final double RETURN_EPSILON = 2;

    /** 驻守点（由装配层经 {@link IGuardPoint} 注入；未注入时=出生点，行为与旧版一致） */
    private double guardX, guardY;

    /** 主动追击拴绳半径 px（= 视野 150 + 30px 余量：可达距离 180−散布 30 = 150 ≥ 需走到的 140，
     *  消除"能锁定却走不到"的抖动带；1/2 级视野 90/120 无需放大） */
    private static final double LEASH_RANGE = 180;

    /** 3 级档案构造：采用 3 级默认数值 */
    public RoyalSoldier(double x, double y) {
        this(x, y, DEFAULT_HP, DEFAULT_SPEED, DEFAULT_ATTACK, DEFAULT_COOLDOWN);
    }

    /** 全参构造：由生产方注入数值，签名与 {@link Soldier} 一致，便于兵营替换生产对象 */
    public RoyalSoldier(double x, double y, int hp, double speed,
                        int attackDamage, int attackCooldown) {
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, hp, speed, attackDamage, attackCooldown);
        this.guardX = x;
        this.guardY = y;
    }

    /** 由装配层（GameController.addAlly）注入驻守点（=离兵营最近的路径点）；实现 {@link IGuardPoint} */
    @Override
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
     * 移动：锁定目标则就位个人环形位并近战（就位才出手）；目标缺失/死亡或离驻守点过远则清空并回防待命。
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

    /** 回防：走回个人岗位（驻守点 + 站岗散布×个人方位角；未注入驻守点时=出生点附近）；到岗则原地待命 */
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
        // 死亡表现：金色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 移除与结算由 GameController 完成，实体不做列表/金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#ffd75e", 16);
    }

    /** 动画帧叠加之后的 3 级标记：动画帧会画满身体框，故在帧之上补画加粗金环 + 盔缨（与色块回退同一套形状）。 */
    @Override
    public void renderPostAnim(GraphicsContext gc) {
        double r = width / 2;
        gc.setStroke(Color.web("#b8860b"));
        gc.setLineWidth(2.5);
        gc.strokeOval(x - r, y - r, width, height);
        gc.setFill(Color.web("#ffd75e"));
        gc.fillOval(x - 2, y - r - 4, 4, 4);
        gc.setFill(Color.web("#e74c3c"));
        gc.fillOval(x - 2.5, y - r - 9, 5, 5);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为深蓝圆身 + 加粗金环 + 红色盔缨（3 级标记）
        if (!drawSprite(gc, AssetKey.ROYAL_SOLDIER)) {
            double r = width / 2;
            gc.setFill(Color.web("#2c6fbb"));        // 3 级底色更深（1/2 级为 #3498db）
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#b8860b"));      // 加粗金环
            gc.setLineWidth(2.5);
            gc.strokeOval(x - r, y - r, width, height);
            gc.setFill(Color.web("#ffd75e"));        // 肩章金星
            gc.fillOval(x - 2, y - r - 4, 4, 4);
            gc.setFill(Color.web("#e74c3c"));        // 红色盔缨：最高档识别标记
            gc.fillOval(x - 2.5, y - r - 9, 5, 5);
        }

        // 头顶血条（沿用友方绿色，语义与 1/2 级士兵一致）
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 8, width, 4);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 8, width * ratio, 4);
    }
}
