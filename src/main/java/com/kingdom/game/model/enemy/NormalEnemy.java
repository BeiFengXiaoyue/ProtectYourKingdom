package com.kingdom.game.model.enemy;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * NormalEnemy —— 基础敌人（红色圆形），沿预设路径走到终点。
 * 数值（HP/速度/赏金）由 GameController 从 GameConfig 取值后经构造函数传入，类内不写死。
 */
public final class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
    }

    @Override
    public void move() {
        followPath();
    }

    @Override
    protected void onDeath() {
        // Day1 尚无攻击方；击杀奖励由 GameController 依 isAlive() 结算
    }

    @Override
    public void render(GraphicsContext gc) {
        double r = width / 2;
        gc.setFill(Color.RED);
        gc.fillOval(x - r, y - r, width, height);
        gc.setStroke(Color.DARKRED);
        gc.setLineWidth(1.5);
        gc.strokeOval(x - r, y - r, width, height);

        // 头顶血条
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 6, width, 3);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 6, width * currentHp / (double) maxHp, 3);
    }
}
