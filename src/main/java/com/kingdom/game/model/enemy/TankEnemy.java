package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;

/**
 * TankEnemy —— 重甲敌人（紫色，血量约为普通敌人 3 倍，体型大）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Enemy → 定数值构造 / onDeath() / render()；移动与拦截复用基类模板。
 */
public class TankEnemy extends Enemy {

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public TankEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
        // TODO 体型：setWidth/setHeight（比普通敌人更大）
        // TODO 近战手感：attackDamage / maxAttackCooldown
    }

    @Override
    protected void onDeath() {
        // TODO 死亡表现（粒子/飘字）；赏金结算在 GameController，此处不处理金币
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.TANK_ENEMY)，缺失回退紫色圆形（可加护甲内环）+ 头顶血条
    }
}
