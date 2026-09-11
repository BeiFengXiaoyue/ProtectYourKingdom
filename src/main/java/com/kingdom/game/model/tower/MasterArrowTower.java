package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterArrowTower —— 大师箭塔（箭塔 3 级，满级）。
 *
 * 继承 {@link EliteArrowTower}（复用 findTarget/attack/render），覆写构造数值与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值来源：构造期从 config/tower.json 读取（TowerBalance），缺省回退硬编码默认。
 * 特殊能力【连锁箭】参数也从 tower.json 读取。
 */
public class MasterArrowTower extends EliteArrowTower implements IChainShot {

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 3;

    @Override
    public int getLevel() { return LEVEL; }

    public MasterArrowTower(double x, double y) {
        super(x, y);
        this.attackRange = TowerBalance.getDouble("arrowL3", "range", 140);
        this.attackCooldown = TowerBalance.getInt("arrowL3", "cooldownMs", 440);
        this.baseAttackDamage = TowerBalance.getInt("arrowL3", "damage", 41);
        this.totalCost = ArrowTower.BUILD_COST
                + ArrowTower.UPGRADE_COST
                + EliteArrowTower.UPGRADE_COST;
        this.nextLevelSpec = null;
    }

    @Override
    public int getChainTargets() { return TowerBalance.getInt("arrowL3", "chainTargets", 1); }

    @Override
    public double getChainDamageRatio() { return TowerBalance.getDouble("arrowL3", "chainDamageRatio", 0.5); }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为塔形色块（三级：金色描边 + 两颗金星）
        if (!drawSprite(gc, AssetKey.ARROW_TOWER)) {
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
