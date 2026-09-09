package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.Soldier;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Barrack —— 兵营（生产士兵拦截敌人，Day4 v0.8 交付物）。
 *
 * 生产流程：update() 定时生成 {@link Soldier}，经自持友方出口 {@link #produce(Ally)} 交给出口；
 * 未接线时默认 no-op，产出被丢弃。
 *
 * 升级链（本类为 1 级，可升级到 {@link EliteBarrack}）：
 * 默认 nextLevelSpec 指向 BARRACK_ELITE；架构师 A 可经 {@link #setNextLevelSpec} 注入/替换链。
 * 生产参数（上限/间隔/士兵属性）为**实例字段**（默认值即 1 级数值），以便 2 级子类覆写差异化。
 *
 * ⚠ 接口状态：本类自带友方出口（仿 Tower.projectileSink 的写法，但**不改 Tower 基类**）。
 * 装配层（GameController.placeTower）接入 {@link #setAllySink(Consumer)} 后，士兵才会真正加入战斗；
 * 在 A 接线前，单独放置的兵营产出的士兵不会进战场。
 *
 * 数值说明（PRD 给定造价 100、3秒/个、上限 3；士兵属性为占位，待《游戏规则说明书》核对）。
 */
public class Barrack extends Tower implements ITowerUpgrade {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 100;

    /** 1 级 → 2 级的升级投入（占位，待《游戏规则说明书》核对；A 可经 setNextLevelSpec 覆盖） */
    public static final int UPGRADE_COST = 120;

    /** 下一级塔目录条目（默认指向 2 级精英兵营；null = 满级） */
    protected TowerSpec nextLevelSpec;

    // ===== 生产与士兵参数（实例字段，默认值即 1 级；2 级子类可覆写）=====
    protected int maxSoldiers = 3;
    protected long spawnIntervalMillis = 3000;
    protected int soldierHp = 70;
    protected double soldierSpeed = 60;
    protected int soldierAttack = 10;
    protected int soldierCooldown = 800;

    /** 友方出口（默认 no-op：未接线时产出被丢弃，不报错） */
    private Consumer<Ally> allySink = a -> { };

    /** 本兵营当前在役士兵（用于上限判断与死亡剪除） */
    private final List<Ally> soldiers = new ArrayList<>();

    /** 生产计时（毫秒，负值/0 表示就绪） */
    private int spawnCooldown = 0;

    public Barrack(double x, double y) {
        super(x, y);
        this.totalCost = BUILD_COST;
        this.upgradeCostBase = 60;
        // 兵营不直接攻击：射程/冷却/伤害均为 0
        this.attackRange = 0;
        this.attackCooldown = 0;
        this.baseAttackDamage = 0;
        this.nextLevelSpec = new TowerSpec(TowerType.BARRACK_ELITE, "精英兵营", UPGRADE_COST);
    }

    /** 由装配层（GameController.placeTower）注入友方出口；传入 null 保持当前 */
    public void setAllySink(Consumer<Ally> sink) {
        if (sink != null) this.allySink = sink;
    }

    /** 由装配层/架构师注入或覆盖升级链（推荐：升级链数据收敛在登记处） */
    public void setNextLevelSpec(TowerSpec spec) {
        if (spec != null) this.nextLevelSpec = spec;
    }

    @Override
    public TowerSpec getNextLevelSpec() { return nextLevelSpec; }

    @Override
    public boolean isMaxLevel() { return nextLevelSpec == null; }

    /** 把生成好的士兵交给出口（update 中调用） */
    protected void produce(Ally soldier) {
        allySink.accept(soldier);
    }

    @Override
    public Enemy findTarget(List<Enemy> enemies) {
        return null;   // 兵营不直接索敌开火
    }

    @Override
    public void attack(Enemy target) {
        // 兵营不直接攻击，生产逻辑在 update() 中
    }

    @Override
    public void update() {
        super.update();               // 处理眩晕与冷却
        if (isStunned) return;        // 眩晕时暂停生产

        // 剪除已阵亡的士兵（存活判定依据 isAlive()）
        soldiers.removeIf(s -> !s.isAlive());

        // 生产计时
        if (spawnCooldown > 0) {
            spawnCooldown -= 16;
        }
        if (spawnCooldown <= 0 && soldiers.size() < maxSoldiers) {
            Soldier soldier = new Soldier(x, y, soldierHp, soldierSpeed,
                    soldierAttack, soldierCooldown);
            soldiers.add(soldier);
            produce(soldier);
            spawnCooldown = (int) spawnIntervalMillis;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为兵营形色块
        if (!drawSprite(gc, AssetKey.BARRACK)) {
            double r = width / 2;
            gc.setFill(Color.web("#62814a"));        // 营房
            gc.fillRect(x - r, y - r, width, height);
            gc.setFill(Color.web("#3c522c"));        // 屋顶
            gc.fillPolygon(new double[]{x - r, x, x + r},
                    new double[]{y - r, y - r - height / 3, y - r}, 3);
            gc.setStroke(Color.DARKGREEN);
            gc.setLineWidth(1.5);
            gc.strokeRect(x - r, y - r, width, height);
        }
    }
}
