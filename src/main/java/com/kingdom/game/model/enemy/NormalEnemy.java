package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * NormalEnemy —— 基础敌人（红色圆形，可替换为 normal_enemy 贴图），沿预设路径走到终点。
 * 数值（HP/速度/赏金）由 GameController 从 GameConfig 取值后经构造函数传入，类内不写死。
 */
public final class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
    }

    @Override
    protected void onDeath() {
        // 死亡表现与金币结算由 GameController 依 isAlive() 统一处理
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为红色圆形
        if (!drawSprite(gc, AssetKey.NORMAL_ENEMY)) {
            double r = width / 2;
            gc.setFill(Color.RED);
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.DARKRED);
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
