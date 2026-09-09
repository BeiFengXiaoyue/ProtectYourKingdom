package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * EliteBarrack —— 精英兵营（兵营 2 级，满级）。
 *
 * 继承 {@link Barrack}（复用生产/剪除逻辑），仅覆写生产与士兵参数 + 渲染加金色标记作区分。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值说明（升级费/累计投入为占位，语义由架构师 A 在 GameController 回填）。
 */
public class EliteBarrack extends Barrack {

    public EliteBarrack(double x, double y) {
        super(x, y);
        this.spawnIntervalMillis = 2000;            // 生产更快 3s → 2s
        this.maxSoldiers = 5;                        // 上限更高 3 → 5
        this.soldierHp = 100;                        // 士兵增强
        this.soldierSpeed = 65;
        this.soldierAttack = 14;
        this.soldierCooldown = 700;
        this.totalCost = Barrack.BUILD_COST + Barrack.UPGRADE_COST;  // 累计投入（占位）
        this.nextLevelSpec = null;                   // 已是满级
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
