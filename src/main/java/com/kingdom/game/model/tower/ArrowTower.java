package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Arrow;
import com.kingdom.game.util.balance.TowerBalance;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * ArrowTower —— 箭塔（基础远程单体塔，Day1 v0.5 交付物）。
 *
 * 发射流程：attack() 生成追踪型 {@link Arrow} 并交给 projectileSink；
 * 命中结算在 Arrow.onHit()（实体负责人 B 的正式交付物）。
 *
 * 升级链（本类为 1 级，可升级到 {@link EliteArrowTower}）：
 * 默认 nextLevelSpec 指向 ARROW_ELITE；架构师 A 可经 {@link #setNextLevelSpec} 注入/替换链。
 *
 * 数值来源：构造期从 config/tower.json 读取（TowerBalance），缺省回退硬编码默认。
 * 改数值请编辑 tower.json，无需改代码。
 */
public class ArrowTower extends Tower implements ITowerUpgrade {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 50;

    /** 1 级 → 2 级的升级投入（《建筑与怪物机制策划》：75；A 可经 setNextLevelSpec 覆盖） */
    public static final int UPGRADE_COST = 75;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 1;

    @Override
    public int getLevel() { return LEVEL; }

    /** 下一级塔目录条目（默认指向 2 级精英箭塔；null = 满级） */
    protected TowerSpec nextLevelSpec;

    /** 箭矢飞行速度 px/s */
    private static final double ARROW_SPEED = 320;

    public ArrowTower(double x, double y) {
        super(x, y);
        this.attackRange = TowerBalance.getDouble("arrowL1", "range", 120);
        this.attackCooldown = TowerBalance.getInt("arrowL1", "cooldownMs", 550);
        this.baseAttackDamage = TowerBalance.getInt("arrowL1", "damage", 18);
        this.totalCost = BUILD_COST;
        this.nextLevelSpec = new TowerSpec(TowerType.ARROW_ELITE, "精英箭塔", UPGRADE_COST);
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
