package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * TankEnemy —— 重甲敌人（紫色，血量约为普通敌人 3 倍，体型大），沿预设路径走到终点。
 * 数值（HP/速度/赏金）由 GameController 从 GameConfig 取值后经构造函数传入，类内不写死。
 * 近战手感：出手重（9）但挥击慢（1400ms 冷却），一击一停的重甲单位。
 */
public class TankEnemy extends Enemy {

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public TankEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
        setWidth(30);   // 未随普通敌人（60）同步放大，已比普通敌人小；如需保持"重甲更大"请上调
        setHeight(30);
        this.attackDamage = 9;
        this.maxAttackCooldown = 1400;
    }

    @Override
    protected void onDeath() {
        // 死亡表现：紫色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 赏金结算由 GameController 依 getGoldReward() 完成，实体不做金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#aa55ff", 12);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为紫色圆形 + 护甲内环
        if (!drawSprite(gc, AssetKey.TANK_ENEMY)) {
            double r = width / 2;
            gc.setFill(Color.web("#9b59b6"));
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#5b2c6f"));   // 外圈重甲描边
            gc.setLineWidth(2);
            gc.strokeOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#c39bd3"));   // 护甲内环
            gc.setLineWidth(1);
            gc.strokeOval(x - r * 0.6, y - r * 0.6, width * 0.6, height * 0.6);
        }

        // 头顶血条
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 8, width, 4);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 8, width * ratio, 4);
    }
}
