package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Bomb;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * CannonTower —— 炮塔（远程群攻塔，Day3 v0.7 交付物）。
 *
 * 发射流程：attack() 生成定点 {@link Bomb} 并交给 projectileSink；
 * 溅射伤害在 Bomb.onHit()（实体负责人 B 的实现）。
 *
 * 数值（PM 要求：不用 final、改构造注入）：本类**不含任何数值常量**，
 * 伤害/射程/冷却/溅射半径/弹速/造价/累计投入/升级链全部取自构造注入的 {@link TowerParams}；
 * 默认值集中在 {@code TowerParamsDefaults.cannonL1()}。
 * ⚠ 溅射半径：`Bomb.onHit()` 内仍硬编码 60px（B 侧尚未支持半径参数），故本类
 * `splashRadius` 当前**未生效**（见《整改方案-文档与代码一致性》§7 P1-1 / 任务 C-2）。
 */
public class CannonTower extends Tower implements ITowerUpgrade {

    /** 下一级塔目录条目（由注入的 params 生成；null = 满级） */
    protected TowerSpec nextLevelSpec;

    /** 爆炸溅射半径 px（来自注入数值；待 Bomb 支持半径参数后传递） */
    protected double splashRadius;

    public CannonTower(double x, double y, TowerParams p) {
        super(x, y);
        applyParams(p);                              // 射程/冷却/伤害/累计投入
        this.splashRadius = p.getSplashRadius();     // 溅射半径（注入）
        this.nextLevelSpec = nextSpecFrom(p);        // 升级链（满级为 null）
    }

    /** 爆炸溅射半径（px） */
    public double getSplashRadius() { return splashRadius; }

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

    /** 开火：从塔中心向目标当前位置发射一枚定点炮弹（弹速取自注入数值） */
    @Override
    public void attack(Enemy target) {
        Bomb bomb = new Bomb(x, y, target.getX(), target.getY(), baseAttackDamage, params.getProjectileSpeed());
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
