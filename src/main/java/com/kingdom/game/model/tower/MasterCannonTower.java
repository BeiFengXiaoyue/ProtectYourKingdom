package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterCannonTower —— 大师炮塔（炮塔 3 级，满级）。
 *
 * 继承 {@link EliteCannonTower}（复用 findTarget/attack），覆写渲染加"三级"标记。
 * 满级：nextLevelSpec = null（由注入数值决定）。
 *
 * 数值（《建筑与怪物机制策划》L3：伤害 110 / 溅射 80）与 3 级能力【燃烧】参数
 * **全部来自构造注入的 {@link TowerParams}**（默认值见 {@code TowerParamsDefaults.cannonL3()}）。
 * 实际 DoT 需敌人侧支持（B 名下）后读取 {@link IBurnEffect} 参数接入。
 */
public class MasterCannonTower extends EliteCannonTower implements IBurnEffect {

    public MasterCannonTower(double x, double y, TowerParams p) {
        super(x, y, p);                              // 数值与满级标记均来自 p
    }

    @Override
    public int getBurnDamagePerSecond() { return params.getBurnDps(); }

    @Override
    public int getBurnDurationMillis() { return params.getBurnDurationMs(); }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为炮塔形色块（三级：金色描边 + 两颗金星）
        if (!drawSprite(gc, AssetKey.CANNON_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#3a3a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#d9b74a"));        // 金色炮管台
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
