package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;

/**
 * FastEnemy —— 快速敌人（黄色，速度约为普通敌人 2 倍，体型略小）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Enemy → 定数值构造 / onDeath() / render()；移动与拦截复用基类模板。
 */
public class FastEnemy extends Enemy {

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public FastEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
        // TODO 体型：setWidth/setHeight（比普通敌人小）
        // TODO 近战手感：attackDamage / maxAttackCooldown（如需要可微调）
    }

    @Override
    protected void onDeath() {
        // TODO 死亡表现（粒子/飘字）；赏金结算在 GameController，此处不处理金币
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.FAST_ENEMY)，缺失回退黄色圆形 + 头顶血条
        //     血条写法可参照 NormalEnemy.render()
    }
}
