package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteCannonTower —— 精英炮塔（炮塔 2 级，满级）。
 *
 * 继承 {@link CannonTower}（复用 findTarget/attack），仅覆写构造数值与渲染加金色标记作区分。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值说明（升级费/累计投入为占位，语义由架构师 A 在 GameController 回填）。
 */
public class EliteCannonTower extends CannonTower {

    public EliteCannonTower(double x, double y) {
        super(x, y);
        this.attackRange = 135;                      // 射程更长
        this.attackCooldown = 700;                   // 冷却更快
        this.baseAttackDamage = 35;                  // 伤害 25 → 35
        this.upgradeCostBase = 70;
        this.totalCost = CannonTower.BUILD_COST + CannonTower.UPGRADE_COST;  // 累计投入（占位）
        this.nextLevelSpec = null;                   // 已是满级
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
