package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.RoyalSoldier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterBarrack —— 大师兵营（兵营 3 级，满级）。
 *
 * 继承 {@link EliteBarrack}（复用生产/剪除逻辑），覆写士兵参数与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值（《建筑与怪物机制策划》L3）：士兵数 4 / 士兵 HP 80（伤害/间隔/速度/重生不变）。
 * 特殊能力【治疗光环】：周围士兵每秒回 2 血 —— 见 {@link IHealAura}，
 * 本次仅暴露参数，实际回血需活体侧支持（B 名下）后接入。
 */
public class MasterBarrack extends EliteBarrack implements IHealAura {

    /** 每秒回血量 */
    private static final int HEAL_PER_SECOND = 2;
    /** 光环半径（px） */
    private static final double AURA_RADIUS = 80;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 3;

    @Override
    public int getLevel() { return LEVEL; }

    public MasterBarrack(double x, double y) {
        super(x, y);
        this.maxSoldiers = 4;                        // 士兵数 3 → 4
        this.soldierHp = 80;                         // 士兵 HP 保持 80
        this.totalCost = Barrack.BUILD_COST
                + Barrack.UPGRADE_COST
                + EliteBarrack.UPGRADE_COST;         // 100 + 100 + 180 = 380
        this.nextLevelSpec = null;                   // 已是满级
    }

    /**
     * 产出兵种：3 级 → {@link RoyalSoldier}。
     * 数值仍取本兵营的等级字段（HP 80 / 速度 40 / 伤害 8 / 冷却 800），**不改任何数值**。
     */
    @Override
    protected Ally createSoldier() {
        return new RoyalSoldier(x, y, soldierHp, soldierSpeed, soldierAttack, soldierCooldown);
    }

    @Override
    public int getHealPerSecond() { return HEAL_PER_SECOND; }

    @Override
    public double getAuraRadius() { return AURA_RADIUS; }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为营房色块（三级：金色屋顶 + 两颗金星）
        if (!drawSprite(gc, AssetKey.MASTER_BARRACK)) {
            double r = width / 2;
            gc.setFill(Color.web("#62814a"));        // 营房
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 金色屋顶
            gc.fillPolygon(new double[]{x - r, x, x + r},
                    new double[]{y - r, y - r - height / 3, y - r}, 3);
            gc.setStroke(Color.web("#8a6d1f"));
            gc.setLineWidth(2);
            gc.strokeRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#ffd75e"));        // 两颗金星（三级标记）
            gc.fillOval(x - 5, y - r - height / 3 - 4, 4, 4);
            gc.fillOval(x + 1, y - r - height / 3 - 4, 4, 4);
        }
    }
}
