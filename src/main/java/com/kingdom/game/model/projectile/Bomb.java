package com.kingdom.game.model.projectile;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Bomb —— 炮弹投射物（定点模式，炮塔用）。
 *
 * 继承 Projectile（选定点构造）：飞行/落点由基类 update()/fly() 完成，
 * 炮弹飞到固定落点后由 GameController 调用一次 {@link #onHit(List)}（AOE 溅射结算），
 * 随后被移出注册表；本类只负责命中结算与渲染。
 */
public class Bomb extends Projectile {

    /** 爆炸溅射半径 px（以落点/弹体位置为中心） */
    private static final double SPLASH_RADIUS = 60;

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
        setWidth(14);   // 炮弹体型（略大于箭矢）
        setHeight(14);
    }

    /**
     * 命中：AOE 溅射——对爆炸中心 SPLASH_RADIUS 内所有存活敌人扣血，
     * 逐个发出命中音效与飘字，并触发一次爆炸粒子（未接线时为空操作）。
     */
    @Override
    public void onHit(List<Enemy> enemies) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (Math.hypot(e.getX() - x, e.getY() - y) <= SPLASH_RADIUS) {
                e.takeDamage(damage);
                combatSound.onProjectileHit(this, e);
                fxText.showFloatingText(e.getX(), e.getY() - 24, "-" + damage, "WHITE");
            }
        }
        fxParticle.spawnExplosionParticles(x, y, "ORANGE", 12);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先
        if (drawSprite(gc, AssetKey.BOMB)) return;
        // 回退：黑色圆形炮弹（中心加一点浅色高光便于看清弹体）
        double r = width / 2.0;
        gc.setFill(Color.BLACK);
        gc.fillOval(x - r, y - r, width, height);
        gc.setFill(Color.web("#666666"));
        gc.fillOval(x - r * 0.35, y - r * 0.35, r * 0.7, r * 0.7);
    }
}
