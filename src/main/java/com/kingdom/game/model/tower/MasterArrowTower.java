package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterArrowTower —— 大师箭塔（箭塔 3 级，满级）。
 *
 * 继承 {@link EliteArrowTower}（复用 findTarget/attack/render），覆写渲染加"三级"标记。
 * 满级：nextLevelSpec = null（由注入数值决定），isMaxLevel() 为 true。
 *
 * 数值（《建筑与怪物机制策划》L3：伤害 41 / 间隔 440ms / 射程 140）与 3 级能力
 * 【连锁箭】参数（目标 1 / 伤害 50%）**全部来自构造注入的 {@link TowerParams}**
 * （默认值见 {@code TowerParamsDefaults.arrowL3()}），本类不含数值常量。
 * 实际连锁逻辑在投射物侧（Arrow.onHit，B 名下）就绪后读取 {@link IChainShot} 参数接入。
 */
public class MasterArrowTower extends EliteArrowTower implements IChainShot {

    public MasterArrowTower(double x, double y, TowerParams p) {
        super(x, y, p);                              // 数值与满级标记均来自 p
    }

    @Override
    public int getChainTargets() { return params.getChainTargets(); }

    @Override
    public double getChainDamageRatio() { return params.getChainDamageRatio(); }

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
