package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.EliteSoldier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteBarrack —— 精英兵营（兵营 2 级）。
 *
 * 继承 {@link Barrack}（复用生产/剪除逻辑），仅覆写"产出兵种"与渲染（金色标记）作区分。
 * 可继续升级到 {@link MasterBarrack}（3 级）。
 *
 * 数值（《建筑与怪物机制策划》L2：士兵数 3 / 士兵 HP 80）全部由构造注入的 {@link TowerParams} 提供
 * （默认值见 {@code TowerParamsDefaults.barrackL2()}），本类不含数值常量。
 */
public class EliteBarrack extends Barrack {

    public EliteBarrack(double x, double y, TowerParams p) {
        super(x, y, p);                              // 数值与升级链均来自 p
    }

    /**
     * 产出兵种：2 级 → {@link EliteSoldier}。
     * 数值仍取本兵营的等级字段（HP 80 / 速度 40 / 伤害 8 / 冷却 800），**不改任何数值**。
     */
    @Override
    protected Ally createSoldier() {
        return new EliteSoldier(x, y, soldierHp, soldierSpeed, soldierAttack, soldierCooldown);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为营房色块（金色屋顶标记，与 1 级区分）
        if (!drawSprite(gc, AssetKey.ELITE_BARRACK)) {
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
