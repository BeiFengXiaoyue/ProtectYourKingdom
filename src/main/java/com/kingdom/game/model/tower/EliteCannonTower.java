package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.GameColors;
import com.kingdom.game.model.IRenderTarget;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.util.balance.BalanceTable;
import com.kingdom.game.util.balance.TowerBalance;

/**
 * EliteCannonTower —— 精英炮塔（炮塔 2 级）。
 *
 * 继承 {@link CannonTower}（复用 findTarget/attack），仅覆写数值与渲染加金色标记作区分。
 * 可继续升级到 {@link MasterCannonTower}（3 级）。
 *
 * 数值来源：伤害从 config/tower.json 读取（TowerBalance）；
 * 溅射半径从 config/balance.json 读取（BalanceTable）。
 */
public class EliteCannonTower extends CannonTower {

    /** 2 级 → 3 级的升级投入（《策划》：250） */
    public static final int UPGRADE_COST = 250;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 2;

    @Override
    public int getLevel() { return LEVEL; }

    public EliteCannonTower(double x, double y) {
        super(x, y);
        this.baseAttackDamage = TowerBalance.getInt("cannonL2", "damage", 98);
        this.splashRadius = BalanceTable.runtime().getEliteCannonSplashRadius();
        this.totalCost = CannonTower.BUILD_COST + CannonTower.UPGRADE_COST;
        this.nextLevelSpec = new TowerSpec(TowerType.CANNON_MASTER, "大师炮塔", UPGRADE_COST);
    }

    @Override
    public void render(IRenderTarget rt) {
        // 贴图优先，未提供素材时回退为炮塔形色块（加金色描边/塔尖标记，与 1 级区分）
        if (!drawSprite(rt, AssetKey.ELITE_CANNON_TOWER)) {
            double r = width / 2;
            rt.setFill(GameColors.TOWER_BASE_CANNON);  // 底座
            rt.fillRect(x - r, y - r, width, height);
            rt.setFill(GameColors.GOLD_PLATE);         // 精英炮管台（金色）
            rt.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            rt.setStroke(GameColors.GOLD_DARK);        // 金色描边
            rt.setLineWidth(2);
            rt.strokeRect(x - r, y - r, width, height);
            // 塔尖金星标记
            rt.setFill(GameColors.GOLD_MARK);
            rt.fillOval(x - 2, y - r - height / 4 - 4, 4, 4);
        }
    }
}
