package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteArrowTower —— 精英箭塔（箭塔 2 级）。
 *
 * 继承 {@link ArrowTower}（复用 findTarget/attack/render），仅覆写构造数值与渲染加金色标记作区分。
 * 可继续升级到 {@link MasterArrowTower}（3 级）。
 *
 * 数值来源：构造期从 config/tower.json 读取（TowerBalance），缺省回退硬编码默认。
 */
public class EliteArrowTower extends ArrowTower {

    /** 2 级 → 3 级的升级投入（《策划》：120） */
    public static final int UPGRADE_COST = 120;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 2;

    @Override
    public int getLevel() { return LEVEL; }

    public EliteArrowTower(double x, double y) {
        super(x, y);
        this.attackRange = TowerBalance.getDouble("arrowL2", "range", 140);
        this.attackCooldown = TowerBalance.getInt("arrowL2", "cooldownMs", 700);
        this.baseAttackDamage = TowerBalance.getInt("arrowL2", "damage", 38);
        this.totalCost = ArrowTower.BUILD_COST + ArrowTower.UPGRADE_COST;
        this.nextLevelSpec = new TowerSpec(TowerType.ARROW_MASTER, "大师箭塔", UPGRADE_COST);
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
