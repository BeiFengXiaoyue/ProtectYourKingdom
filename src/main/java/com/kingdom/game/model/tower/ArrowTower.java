package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Arrow;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * ArrowTower —— 箭塔（基础远程单体塔，Day1 v0.5 交付物）。
 *
 * 发射流程：attack() 生成追踪型 {@link Arrow} 并交给 projectileSink；
 * 命中结算在 Arrow.onHit()（实体负责人 B 的正式交付物）。
 *
 * 数值说明（开发期可直接在构造函数微调）：
 * - 对照默认普通敌人 HP80 / 初始金币100：造价 50 可起手两座，
 *   伤害 18 + 冷却 550ms 约 4~5 箭击杀一只普通敌人。
 * - BUILD_COST 与装配处 TowerSpec 的造价保持一致（出售返还依赖 totalCost）。
 */
public class ArrowTower extends Tower {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 50;

    /** 箭矢飞行速度 px/s */
    private static final double ARROW_SPEED = 320;

    public ArrowTower(double x, double y) {
        super(x, y);
        this.attackRange = 120;
        this.attackCooldown = 550;
        this.baseAttackDamage = 18;
        this.upgradeCostBase = 45;
        this.totalCost = BUILD_COST;
    }

    /**
     * 索敌：返回射程内第一个存活敌人。
     * 敌人列表顺序即出生顺序（无超车），故"第一个入圈"即当前最靠前的敌人。
     */
    @Override
    public Enemy findTarget(List<Enemy> enemies) {
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            if (Math.hypot(e.getX() - x, e.getY() - y) <= attackRange) {
                return e;
            }
        }
        return null;
    }

    /** 开火：从塔中心生成一枚追踪箭矢并发射 */
    @Override
    public void attack(Enemy target) {
        Arrow arrow = new Arrow(x, y, target, baseAttackDamage, ARROW_SPEED);
        combatSound.onProjectileFired(arrow);
        fire(arrow);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为塔形色块
        if (!drawSprite(gc, AssetKey.ARROW_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#5b4a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#8a7a5c"));        // 顶台
            gc.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            gc.setStroke(Color.DARKGRAY);
            gc.setLineWidth(1.5);
            gc.strokeRect(x - r, y - r, width, height);
        }
    }
}
