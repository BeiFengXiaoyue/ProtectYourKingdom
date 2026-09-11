package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterArrowTower —— 大师箭塔（箭塔 3 级，满级）。
 *
 * 继承 {@link EliteArrowTower}（复用 findTarget/attack/render），覆写构造数值与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值（《建筑与怪物机制策划》L3）：伤害 41 / 间隔 440ms（+20% 攻速）/ 射程 140。
 * 特殊能力【连锁箭】：主目标命中后溅射附近 1 个敌人，伤害 50% —— 见 {@link IChainShot}，
 * 本次仅暴露参数，实际连锁在投射物侧（Arrow.onHit，B 名下）就绪后接入。
 */
public class MasterArrowTower extends EliteArrowTower implements IChainShot {

    /** 连锁目标数 */
    private static final int CHAIN_TARGETS = 1;
    /** 连锁伤害比例 */
    private static final double CHAIN_DAMAGE_RATIO = 0.5;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 3;

    @Override
    public int getLevel() { return LEVEL; }

    public MasterArrowTower(double x, double y) {
        super(x, y);
        this.attackRange = 140;                      // 射程保持 140
        this.attackCooldown = 440;                   // 间隔 550 → 440（+20% 攻速）
        this.baseAttackDamage = 41;                  // 伤害 27 → 41
        this.totalCost = ArrowTower.BUILD_COST
                + ArrowTower.UPGRADE_COST
                + EliteArrowTower.UPGRADE_COST;      // 50 + 75 + 120 = 245
        this.nextLevelSpec = null;                   // 已是满级
    }

    @Override
    public int getChainTargets() { return CHAIN_TARGETS; }

    @Override
    public double getChainDamageRatio() { return CHAIN_DAMAGE_RATIO; }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为塔形色块（三级：金色描边 + 两颗金星）
        if (!drawSprite(gc, AssetKey.MASTER_ARROW_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#5b4a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 金色台
            gc.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            gc.setStroke(Color.web("#8a6d1f"));
            gc.setLineWidth(2);
            gc.strokeRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#ffd75e"));        // 两颗金星（三级标记）
            gc.fillOval(x - 5, y - r - height / 4 - 4, 4, 4);
            gc.fillOval(x + 1, y - r - height / 4 - 4, 4, 4);
        }
    }
}
