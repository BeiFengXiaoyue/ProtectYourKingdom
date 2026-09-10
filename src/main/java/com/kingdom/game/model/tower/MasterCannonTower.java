package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterCannonTower —— 大师炮塔（炮塔 3 级，满级）。
 *
 * 继承 {@link EliteCannonTower}（复用 findTarget/attack），覆写数值与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值（《建筑与怪物机制策划》L3）：伤害 110 / 溅射 80（间隔 1500ms、射程 140 不变）。
 * 特殊能力【燃烧】：命中后敌人每秒受 5 点伤害、持续 3 秒 —— 见 {@link IBurnEffect}，
 * 本次仅暴露参数，实际 DoT 需敌人侧支持（B 名下）后接入。
 */
public class MasterCannonTower extends EliteCannonTower implements IBurnEffect {

    /** 燃烧每秒伤害 */
    private static final int BURN_DPS = 5;
    /** 燃烧持续时间（毫秒） */
    private static final int BURN_DURATION_MILLIS = 3000;

    public MasterCannonTower(double x, double y) {
        super(x, y);
        this.baseAttackDamage = 110;                 // 伤害 70 → 110
        this.splashRadius = 80;                      // 溅射 65 → 80
        this.upgradeCostBase = 90;
        this.totalCost = CannonTower.BUILD_COST
                + CannonTower.UPGRADE_COST
                + EliteCannonTower.UPGRADE_COST;     // 80 + 150 + 250 = 480
        this.nextLevelSpec = null;                   // 已是满级
    }

    @Override
    public int getBurnDamagePerSecond() { return BURN_DPS; }

    @Override
    public int getBurnDurationMillis() { return BURN_DURATION_MILLIS; }

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
