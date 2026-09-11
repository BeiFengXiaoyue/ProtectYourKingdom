package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.util.balance.BalanceTable;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * MasterCannonTower —— 大师炮塔（炮塔 3 级，满级）。
 *
 * 继承 {@link EliteCannonTower}（复用 findTarget/attack），覆写数值与渲染加"三级"标记。
 * 满级：nextLevelSpec = null，isMaxLevel() 为 true。
 *
 * 数值来源：伤害从 config/tower.json 读取（TowerBalance）；
 * 溅射半径从 config/balance.json 读取（BalanceTable）；
 * 特殊能力【燃烧】参数也从 tower.json 读取。
 */
public class MasterCannonTower extends EliteCannonTower implements IBurnEffect {

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 3;

    @Override
    public int getLevel() { return LEVEL; }

    public MasterCannonTower(double x, double y) {
        super(x, y);
        this.baseAttackDamage = TowerBalance.getInt("cannonL3", "damage", 110);
        this.splashRadius = BalanceTable.runtime().getMasterCannonSplashRadius();
        this.totalCost = CannonTower.BUILD_COST
                + CannonTower.UPGRADE_COST
                + EliteCannonTower.UPGRADE_COST;
        this.nextLevelSpec = null;
    }

    @Override
    public int getBurnDamagePerSecond() { return TowerBalance.getInt("cannonL3", "burnDps", 5); }

    @Override
    public int getBurnDurationMillis() { return TowerBalance.getInt("cannonL3", "burnDurationMs", 3000); }

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
