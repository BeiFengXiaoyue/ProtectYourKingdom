package com.kingdom.game.model.projectile;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * Bomb —— 炮弹投射物（定点模式，炮塔用）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Projectile（选定点构造）→ onHit() / render()；
 * 飞行/落点由基类 update()/fly() 完成。
 */
public class Bomb extends Projectile {

    /**
     * 定点模式构造：飞向固定落点 (targetX, targetY) 后爆炸。
     *
     * @param targetX 落点 x
     * @param targetY 落点 y
     * @param damage  命中伤害
     * @param speed   飞行速度 px/s
     */
    public Bomb(double x, double y, double targetX, double targetY, int damage, double speed) {
        super(x, y, targetX, targetY, damage, speed);
        // TODO 体型：setWidth/setHeight
    }

    @Override
    public void onHit(List<Enemy> enemies) {
        // TODO 命中效果：对落点附近（如半径 60px 内）所有存活敌人 takeDamage(damage)（AOE 溅射）
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.BOMB)，缺失回退为黑色圆形炮弹
    }
}
