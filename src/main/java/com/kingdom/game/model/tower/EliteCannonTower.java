package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteCannonTower —— 精英炮塔（炮塔 2 级）。
 *
 * 继承 {@link CannonTower}（复用 findTarget/attack），仅覆写渲染加金色标记作区分。
 * 可继续升级到 {@link MasterCannonTower}（3 级）。
 *
 * 数值（《建筑与怪物机制策划》L2：伤害 70 / 溅射 65）全部由构造注入的 {@link TowerParams} 提供
 * （默认值见 {@code TowerParamsDefaults.cannonL2()}），本类不含数值常量。
 */
public class EliteCannonTower extends CannonTower {

    public EliteCannonTower(double x, double y, TowerParams p) {
        super(x, y, p);                              // 数值与升级链均来自 p
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为炮塔形色块（加金色描边/塔尖标记，与 1 级区分）
        if (!drawSprite(gc, AssetKey.CANNON_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#3a3a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 精英炮管台（金色）
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
