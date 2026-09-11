package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.TowerSpec;
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
 * 数值（PM 要求：不用 final、改构造注入）：本类**不含任何数值常量**，
 * 伤害/射程/冷却/造价/累计投入/箭矢速度/升级链全部取自构造注入的 {@link TowerParams}；
 * 默认值集中在 {@code TowerParamsDefaults.arrowL1()}，接线人员改数值只改那一处。
 */
public class ArrowTower extends Tower implements ITowerUpgrade {

    /** 下一级塔目录条目（由注入的 params 生成；null = 满级） */
    protected TowerSpec nextLevelSpec;

    public ArrowTower(double x, double y, TowerParams p) {
        super(x, y);
        applyParams(p);                              // 射程/冷却/伤害/累计投入
        this.nextLevelSpec = nextSpecFrom(p);        // 升级链（满级为 null）
    }

    /** 由装配层/架构师注入或覆盖升级链（推荐：升级链数据收敛在登记处） */
    public void setNextLevelSpec(TowerSpec spec) {
        if (spec != null) this.nextLevelSpec = spec;
    }

    @Override
    public TowerSpec getNextLevelSpec() { return nextLevelSpec; }

    @Override
    public boolean isMaxLevel() { return nextLevelSpec == null; }

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

    /** 开火：从塔中心生成一枚追踪箭矢并发射（箭速取自注入数值） */
    @Override
    public void attack(Enemy target) {
        Arrow arrow = new Arrow(x, y, target, baseAttackDamage, params.getProjectileSpeed());
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
