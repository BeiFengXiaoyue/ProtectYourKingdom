package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteBarrack —— 精英兵营（兵营 2 级）。
 *
 * 继承 {@link Barrack}（复用生产/剪除逻辑），仅覆写士兵参数 + 渲染加金色标记作区分。
 * 可继续升级到 {@link MasterBarrack}（3 级）。
 *
 * 数值（《建筑与怪物机制策划》L2）：士兵数 2 → 3、士兵 HP 50 → 80（伤害/间隔/速度/重生不变）。
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
        this.maxSoldiers = 3;                        // 士兵数 2 → 3
        this.soldierHp = 80;                         // 士兵 HP 50 → 80
        this.totalCost = Barrack.BUILD_COST + Barrack.UPGRADE_COST;      // 100 + 100 = 200
        this.nextLevelSpec = new TowerSpec(TowerType.BARRACK_MASTER, "大师兵营", UPGRADE_COST);
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
