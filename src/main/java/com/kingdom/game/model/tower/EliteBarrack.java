package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.EliteSoldier;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteBarrack —— 精英兵营（兵营 2 级）。
 *
 * 继承 {@link Barrack}（复用生产/剪除逻辑），仅覆写士兵参数 + 渲染加金色标记作区分。
 * 可继续升级到 {@link MasterBarrack}（3 级）。
 *
 * 数值来源：构造期从 config/tower.json 的 barrackL2 读取（TowerBalance）。
 */
public class EliteBarrack extends Barrack {

    /** 2 级 → 3 级的升级投入（《策划》：180） */
    public static final int UPGRADE_COST = 180;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 2;

    @Override
    public int getLevel() { return LEVEL; }

    public EliteBarrack(double x, double y) {
        super(x, y);
        this.maxSoldiers = TowerBalance.getInt("barrackL2", "maxSoldiers", 3);
        this.spawnIntervalMillis = (long) TowerBalance.getInt("barrackL2", "spawnIntervalMs", 5000);
        this.soldierHp = TowerBalance.getInt("barrackL2", "soldierHp", 80);
        this.soldierSpeed = TowerBalance.getDouble("barrackL2", "soldierSpeed", 40);
        this.soldierAttack = TowerBalance.getInt("barrackL2", "soldierAttack", 8);
        this.soldierCooldown = TowerBalance.getInt("barrackL2", "soldierCooldown", 800);
        this.totalCost = Barrack.BUILD_COST + Barrack.UPGRADE_COST;
        this.nextLevelSpec = new TowerSpec(TowerType.BARRACK_MASTER, "大师兵营", UPGRADE_COST);
    }

    /**
     * 产出兵种：2 级 → {@link EliteSoldier}。
     * 数值仍取本兵营的等级字段，**不改任何数值**。
     */
    @Override
    protected Ally createSoldier() {
        return new EliteSoldier(x, y, soldierHp, soldierSpeed, soldierAttack, soldierCooldown);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为营房色块（金色屋顶标记，与 1 级区分）
        if (!drawSprite(gc, AssetKey.BARRACK)) {
            double r = width / 2;
            gc.setFill(Color.web("#62814a"));        // 营房
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 精英屋顶（金色）
            gc.fillPolygon(new double[]{x - r, x, x + r},
                    new double[]{y - r, y - r - height / 3, y - r}, 3);
            gc.setStroke(Color.web("#8a6d1f"));      // 金色描边
            gc.setLineWidth(2);
            gc.strokeRect(x - r, y - r, width, height);
            // 屋顶金星标记
            gc.setFill(Color.web("#ffd75e"));
            gc.fillOval(x - 2, y - r - height / 3 - 4, 4, 4);
        }
    }
}
