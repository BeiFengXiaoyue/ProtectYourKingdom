package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.GameColors;
import com.kingdom.game.model.IRenderTarget;
import com.kingdom.game.util.balance.TowerBalance;

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
        this.attackCooldown = TowerBalance.getInt("arrowL3", "cooldownMs", 600);
        this.baseAttackDamage = TowerBalance.getInt("arrowL3", "damage", 57);
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
    public void render(IRenderTarget rt) {
        // 贴图优先，未提供素材时回退为塔形色块（三级：金色描边 + 两颗金星）
        if (!drawSprite(rt, AssetKey.MASTER_ARROW_TOWER)) {
            double r = width / 2;
            rt.setFill(GameColors.TOWER_BASE_ARROW);  // 底座
            rt.fillRect(x - r, y - r, width, height);
            rt.setFill(GameColors.GOLD_PLATE);        // 金色台
            rt.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            rt.setStroke(GameColors.GOLD_DARK);
            rt.setLineWidth(2);
            rt.strokeRect(x - r, y - r, width, height);
            rt.setFill(GameColors.GOLD_MARK);         // 两颗金星（三级标记）
            rt.fillOval(x - 5, y - r - height / 4 - 4, 4, 4);
            rt.fillOval(x + 1, y - r - height / 4 - 4, 4, 4);
        }
    }
}
