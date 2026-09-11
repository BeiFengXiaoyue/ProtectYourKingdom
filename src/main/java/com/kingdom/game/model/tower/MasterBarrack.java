package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.RoyalSoldier;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterBarrack —— 大师兵营（兵营 3 级，满级）。
 *
 * 继承 {@link EliteBarrack}（复用生产/剪除逻辑），覆写士兵参数与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值来源：构造期从 config/tower.json 的 barrackL3 读取（TowerBalance）；
 * 特殊能力【治疗光环】参数也从 tower.json 读取。
 */
public class MasterBarrack extends EliteBarrack implements IHealAura {

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 3;

    @Override
    public int getLevel() { return LEVEL; }

    public MasterBarrack(double x, double y) {
        super(x, y);
        this.maxSoldiers = TowerBalance.getInt("barrackL3", "maxSoldiers", 4);
        this.spawnIntervalMillis = (long) TowerBalance.getInt("barrackL3", "spawnIntervalMs", 5000);
        this.soldierHp = TowerBalance.getInt("barrackL3", "soldierHp", 80);
        this.soldierSpeed = TowerBalance.getDouble("barrackL3", "soldierSpeed", 40);
        this.soldierAttack = TowerBalance.getInt("barrackL3", "soldierAttack", 11);
        this.soldierCooldown = TowerBalance.getInt("barrackL3", "soldierCooldown", 800);
        this.totalCost = Barrack.BUILD_COST
                + Barrack.UPGRADE_COST
                + EliteBarrack.UPGRADE_COST;
        this.nextLevelSpec = null;
    }

    /**
     * 产出兵种：3 级 → {@link RoyalSoldier}。
     * 数值仍取本兵营的等级字段，**不改任何数值**。
     */
    @Override
    protected Ally createSoldier() {
        return new RoyalSoldier(x, y, soldierHp, soldierSpeed, soldierAttack, soldierCooldown);
    }

    @Override
    public int getHealPerSecond() { return TowerBalance.getInt("barrackL3", "healPerSecond", 2); }

    @Override
    public double getAuraRadius() { return TowerBalance.getDouble("barrackL3", "auraRadius", 80); }

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
