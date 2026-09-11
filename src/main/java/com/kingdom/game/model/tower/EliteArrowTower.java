package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteArrowTower —— 精英箭塔（箭塔 2 级）。
 *
 * 继承 {@link ArrowTower}（复用 findTarget/attack/render），仅覆写渲染加金色标记作区分。
 * 可继续升级到 {@link MasterArrowTower}（3 级）。
 *
 * 数值（《建筑与怪物机制策划》L2：伤害 27 / 间隔 550ms / 射程 140）不再写在本类，
 * 全部由构造注入的 {@link TowerParams} 提供（默认值见 {@code TowerParamsDefaults.arrowL2()}）。
 */
public class EliteArrowTower extends ArrowTower {

    public EliteArrowTower(double x, double y, TowerParams p) {
        super(x, y, p);                              // 数值与升级链均来自 p
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为塔形色块（加金色描边/塔尖标记，与 1 级区分）
        if (!drawSprite(gc, AssetKey.ELITE_ARROW_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#5b4a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 精英台（金色）
            gc.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            gc.setStroke(Color.web("#8a6d1f"));      // 金色描边
            gc.setLineWidth(2);
            gc.strokeRect(x - r, y - r, width, height);
            // 塔尖金星标记
            gc.setFill(Color.web("#ffd75e"));
            gc.fillOval(x - 2, y - r - height / 4 - 4, 4, 4);
        }
    }
}
