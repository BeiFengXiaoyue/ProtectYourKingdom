package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.GameColors;
import com.kingdom.game.model.IRenderTarget;
import com.kingdom.game.util.balance.BalanceTable;

/**
 * TankEnemy —— 重甲敌人（紫色，血量约为普通敌人 3 倍，体型大），沿预设路径走到终点。
 * 数值（HP/速度/赏金/攻击力/攻击冷却）由调用方取值后经构造函数传入，类内不写死。
 * 近战手感：出手重（9）但挥击慢（1400ms 冷却），一击一停的重甲单位。
 */
public class TankEnemy extends Enemy {

    /**
     * 老签名（保留重载，B-4）：近战参数从 config/balance.json 取
     * （`tankAttackDamage` / `tankAttackCooldownMs`，缺省回退 9 / 1400）。
     *
     * @param hp         生命值
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public TankEnemy(double x, double y, int hp, double speed, int goldReward) {
        this(x, y, hp, speed, goldReward,
                BalanceTable.runtime().getTankAttackDamage(),
                BalanceTable.runtime().getTankAttackCooldownMs());
    }

    /**
     * B-4 扩参构造：近战参数由调用方（`GameController.spawnEnemy` 经 `GameConfig`）显式注入。
     *
     * @param attackDamage      近战攻击力（≥1）
     * @param attackCooldownMs  近战攻击冷却 ms（≥1）
     */
    public TankEnemy(double x, double y, int hp, double speed, int goldReward,
                     int attackDamage, int attackCooldownMs) {
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, hp, speed, goldReward);
        this.attackDamage = attackDamage;
        this.maxAttackCooldown = attackCooldownMs;
        // 重甲物理减伤：构造期从 config/balance.json 的 tankPhysicalReduction 读入并固化。
        // 倍率 = 1 − tankPhysicalReduction（只吃剩余比例伤害）；
        // 该值只作用于"未声明破甲"的伤害——炮塔 Bomb 走 takeDamage(dmg, true) 绕过。
        this.damageTakenMultiplier =
                1.0 - BalanceTable.runtime().getTankPhysicalReduction();
    }

    @Override
    protected void onDeath() {
        // 死亡表现：紫色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 赏金结算由 GameController 依 getGoldReward() 完成，实体不做金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#aa55ff", 12);
    }

    @Override
    public void render(IRenderTarget rt) {
        // 贴图优先，未提供素材时回退为紫色圆形 + 护甲内环
        if (!drawSprite(rt, AssetKey.TANK_ENEMY)) {
            double r = width / 2;
            rt.setFill("#9b59b6");
            rt.fillOval(x - r, y - r, width, height);
            rt.setStroke("#5b2c6f");   // 外圈重甲描边
            rt.setLineWidth(2);
            rt.strokeOval(x - r, y - r, width, height);
            rt.setStroke("#c39bd3");   // 护甲内环
            rt.setLineWidth(1);
            rt.strokeOval(x - r * 0.6, y - r * 0.6, width * 0.6, height * 0.6);
        }

        // 头顶血条
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        rt.setFill(GameColors.HP_BAR_BG);
        rt.fillRect(x - r, y - r - 8, width, 4);
        rt.setFill(GameColors.HP_BAR_FILL);
        rt.fillRect(x - r, y - r - 8, width * ratio, 4);
    }
}
