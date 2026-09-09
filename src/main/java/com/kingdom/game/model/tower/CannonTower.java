package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Bomb;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * CannonTower —— 炮塔（远程群攻塔，Day3 v0.7 交付物）。
 *
 * 发射流程：attack() 生成定点 {@link Bomb} 并交给 projectileSink；
 * 溅射伤害在 Bomb.onHit()（实体负责人 B 的实现，~半径 60px 内所有敌人）。
 *
 * 升级链（本类为 1 级，可升级到 {@link EliteCannonTower}）：
 * 默认 nextLevelSpec 指向 CANNON_ELITE；架构师 A 可经 {@link #setNextLevelSpec} 注入/替换链。
 *
 * 数值说明（开发期可调；PRD 仅给定造价 80，其余为占位，待《游戏规则说明书》核对）：
 * - 射程/冷却/伤害为可调占位；造价 80 与装配处 TowerSpec 保持一致（出售返还依赖 totalCost）。
 */
public class CannonTower extends Tower implements ITowerUpgrade {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 80;

    /** 1 级 → 2 级的升级投入（占位，待《游戏规则说明书》核对；A 可经 setNextLevelSpec 覆盖） */
    public static final int UPGRADE_COST = 100;

    /** 下一级塔目录条目（默认指向 2 级精英炮塔；null = 满级） */
    protected TowerSpec nextLevelSpec;

    /** 炮弹飞行速度 px/s */
    private static final double BOMB_SPEED = 220;

    public CannonTower(double x, double y) {
        super(x, y);
        this.attackRange = 120;
        this.attackCooldown = 900;
        this.baseAttackDamage = 25;
        this.upgradeCostBase = 50;
        this.totalCost = BUILD_COST;
        this.nextLevelSpec = new TowerSpec(TowerType.CANNON_ELITE, "精英炮塔", UPGRADE_COST);
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
     * 索敌：返回射程内最近存活敌人（炮弹为定点，故选最近目标生成落点）。
     */
    @Override
    public Enemy findTarget(List<Enemy> enemies) {
        Enemy nearest = null;
        double best = Double.MAX_VALUE;
        for (Enemy e : enemies) {
            if (!e.isAlive()) continue;
            double d = Math.hypot(e.getX() - x, e.getY() - y);
            if (d <= attackRange && d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    /** 开火：从塔中心向目标当前位置发射一枚定点炮弹 */
    @Override
    public void attack(Enemy target) {
        Bomb bomb = new Bomb(x, y, target.getX(), target.getY(), baseAttackDamage, BOMB_SPEED);
        combatSound.onProjectileFired(bomb);
        fire(bomb);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为炮塔形色块
        if (!drawSprite(gc, AssetKey.CANNON_TOWER)) {
            double r = width / 2;
            gc.setFill(Color.web("#3a3a3a"));        // 底座
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#666666"));        // 炮管/炮塔顶
            gc.fillOval(x - r, y - r - height / 4, width * 0.9, height * 0.6);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1.5);
            gc.strokeRect(x - r, y - r, width, height);
        }
    }
}
