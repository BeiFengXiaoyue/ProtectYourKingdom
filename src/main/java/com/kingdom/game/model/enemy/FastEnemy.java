package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * FastEnemy —— 快速敌人（黄色，速度约为普通敌人 2 倍，体型略小），沿预设路径走到终点。
 * 数值（HP/速度/赏金）由 GameController 从 GameConfig 取值后经构造函数传入，类内不写死。
 * 近战手感：攻速快（700ms 冷却）但伤害低（4），骚扰型单位。
 */
public class FastEnemy extends Enemy {

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public FastEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
        setWidth(16);   // 比普通敌人（20）更小
        setHeight(16);
        this.attackDamage = 4;
        this.maxAttackCooldown = 700;
    }

    @Override
    protected void onDeath() {
        // 死亡表现：黄色粒子爆散（经事件槽，未接特效层时为空操作）
        // 死亡音效 onUnitDied 已在基类 LivingEntity.takeDamage() 触发；
        // 赏金结算由 GameController 依 getGoldReward() 完成，实体不做金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#ffcc00", 6);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为黄色圆形
        if (!drawSprite(gc, AssetKey.FAST_ENEMY)) {
            double r = width / 2;
            gc.setFill(Color.YELLOW);
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#cc9900"));
            gc.setLineWidth(1.5);
            gc.strokeOval(x - r, y - r, width, height);
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
