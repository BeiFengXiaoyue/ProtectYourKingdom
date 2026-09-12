package com.kingdom.game.model.ally;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * EliteSoldier —— 精英士兵（士兵 **2 级**）。
 *
 * 分级方式：1 级 {@link Soldier}、2 级本类、3 级 {@link RoyalSoldier} 是三个**并列**的具体单位，
 * 都直接继承 {@link Ally}，**互不继承**——每级的数值/视野/表现各自独立，改一级不影响其他级；
 * 代价是索敌/追击/回防三段逻辑各级各写一份，按项目“实体类自包含”的既有口径可接受。
 *
 * 行为（与 1 级士兵同一套框架）：
 * - 索敌：findTarget() 返回视野（VISION_RANGE）内最近存活敌人，供 GameController 每帧轮询；
 * - 追击：target 存活则贴身至 MELEE_RANGE 并近战（tryAttack 内部按冷却节拍）；
 * - 回防：target 为 null/死亡时回到出生锚点 (homeX, homeY) 待命；
 * - 碰撞拦截由基类 Ally.handleEntityCollision() 完成（接触即锁定目标 + 近战反击）。
 *
 * 2 级档默认数值：HP 100 / 速度 65 / 攻击 14 / 冷却 700ms / 视野 120px / 体型 20×20
 * （1 级 70 / 60 / 10 / 800ms / 90px / 18×18）。该档与 {@link com.kingdom.game.model.tower.EliteBarrack}
 * 当前的生产参数一致，因此把 2 级兵营的产出换成本类**不改变战斗数值**。
 *
 * 数值归属：与 Soldier 一致，生产方（兵营/装配层）可经全参构造注入；无参档案构造只是
 * “直接实例化”时的兜底，不是唯一数值源。
 */
public class EliteSoldier extends Ally implements IGuardPoint {

    // ===== 2 级默认档案（1 级 → 2 级：HP 70→100、速度 60→65、攻击 10→14、冷却 800→700）=====
    /** 2 级默认生命值 */
    public static final int DEFAULT_HP = 100;
    /** 2 级默认移动速度 px/s */
    public static final double DEFAULT_SPEED = 65;
    /** 2 级默认攻击力 */
    public static final int DEFAULT_ATTACK = 14;
    /** 2 级默认攻击冷却 ms */
    public static final int DEFAULT_COOLDOWN = 700;

    /** 索敌视野半径 px（1 级 90 → 2 级 120） */
    private static final double VISION_RANGE = 120;

    /** 贴身近战距离 px（与 Enemy.chaseAndAttack / Soldier.MELEE_RANGE 一致，勿单独调整） */
    private static final double MELEE_RANGE = 10;

    /** 回防到家判定距离 px */
    private static final double RETURN_EPSILON = 2;

    /** 出生锚点 */
    private final double homeX, homeY;

    /**
     * 驻守点（由装配层经 {@link IGuardPoint} 注入；未注入时=出生锚点）。
     * ⚠️ 本批（接口落地批）仅承接注入、字段暂未被 {@code move()}/{@code returnHome()} 读取，
     * 故二三级兵当前行为不变；改为"回防/拴绳/环形就位读驻守点"属**行为对齐第二批**（归实体侧）。
     */
    private double guardX, guardY;

    /** 2 级档案构造：采用 2 级默认数值 */
    public EliteSoldier(double x, double y) {
        this(x, y, DEFAULT_HP, DEFAULT_SPEED, DEFAULT_ATTACK, DEFAULT_COOLDOWN);
    }

    /** 全参构造：由生产方注入数值，签名与 {@link Soldier} 一致，便于兵营替换生产对象 */
    public EliteSoldier(double x, double y, int hp, double speed,
                        int attackDamage, int attackCooldown) {
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, hp, speed, attackDamage, attackCooldown);
        this.homeX = x;
        this.homeY = y;
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
        // 死亡表现：金色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 移除与结算由 GameController 完成，实体不做列表/金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#ffd75e", 12);
    }

    /**
     * 动画帧叠加之后的精英标记：叠加层会把行为动画帧画满整个身体框，
     * 无素材时 render() 里那套标记会被盖住，故在帧之上再画一遍（同一套形状，颜色/位置完全一致）。
     */
    @Override
    public void renderPostAnim(GraphicsContext gc) {
        double r = width / 2;
        gc.setStroke(Color.web("#8a6d1f"));      // 精英金环
        gc.setLineWidth(2);
        gc.strokeOval(x - r, y - r, width, height);
        gc.setFill(Color.web("#ffd75e"));        // 肩章金星
        gc.fillOval(x - 2, y - r - 4, 4, 4);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为蓝色圆身 + 金色精英环
        if (!drawSprite(gc, AssetKey.ELITE_SOLDIER)) {
            double r = width / 2;
            gc.setFill(Color.web("#3498db"));
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#8a6d1f"));      // 精英金边（1 级为深蓝边）
            gc.setLineWidth(2);
            gc.strokeOval(x - r, y - r, width, height);
            gc.setFill(Color.web("#ffd75e"));        // 肩章金星
            gc.fillOval(x - 2, y - r - 4, 4, 4);
        }

        // 头顶血条（沿用友方绿色，语义与 1 级士兵一致）
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 8, width, 4);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 8, width * ratio, 4);
    }
}
