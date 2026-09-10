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
 * 生产流程：为每个槽位（共 maxSoldiers 个）独立计时，经自持友方出口 {@link #produce(Ally)} 交给出口；
 * **士兵阵亡后该槽位独立重新计时 spawnIntervalMillis 再补位**（不再"死亡立即复活"），其它槽位不受影响；
 * 初始错峰产出（0 / 间隔 / 2×间隔 …）；未接线时默认 no-op，产出被丢弃。
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

    /** 1 级 → 2 级的升级投入（《建筑与怪物机制策划》：100；A 可经 setNextLevelSpec 覆盖） */
    public static final int UPGRADE_COST = 100;

    /** 下一级塔目录条目（默认指向 2 级精英兵营；null = 满级） */
    protected TowerSpec nextLevelSpec;

    // ===== 生产与士兵参数（实例字段，默认值即 1 级；2/3 级子类可覆写）=====
    // 数值来源《建筑与怪物机制策划》兵营基础：士兵2 / HP50 / 伤害8 / 攻击间隔800ms / 速度40 / 重生5s（覆盖文档10s）
    protected int maxSoldiers = 2;
    protected long spawnIntervalMillis = 5000;
    protected int soldierHp = 50;
    protected double soldierSpeed = 40;
    protected int soldierAttack = 8;
    protected int soldierCooldown = 800;

    /** 友方出口（默认 no-op：未接线时产出被丢弃，不报错） */
    private Consumer<Ally> allySink = a -> { };

    /** 生产槽位（数量 = maxSoldiers）：每个槽位各带独立补位计时器 */
    private final List<SoldierSlot> slots = new ArrayList<>();

    /**
     * 生产槽位：士兵阵亡后本槽位独立重新计时，归零才补位。
     * soldier 为当前占用者（null = 空槽/待补）；respawnTimer 仅在本槽位无存活士兵时递减。
     */
    private static final class SoldierSlot {
        Ally soldier;
        int respawnTimer = 0;
    }

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
        ensureSlots();

        // 逐槽维护：每个槽位独立计时，士兵阵亡后本槽位重新计时才补位
        for (SoldierSlot slot : slots) {
            if (slot.soldier != null && slot.soldier.isAlive()) continue;  // 该槽位有存活士兵

            if (slot.soldier != null) {            // 士兵刚阵亡 → 本槽位开始独立计时
                slot.soldier = null;
                slot.respawnTimer = (int) spawnIntervalMillis;
            }
            if (slot.respawnTimer > 0) {
                slot.respawnTimer -= 16;           // 沿用 16ms/帧 约定
            }
            if (slot.respawnTimer <= 0) {          // 计时归零 → 补位
                Soldier soldier = new Soldier(x, y, soldierHp, soldierSpeed,
                        soldierAttack, soldierCooldown);
                slot.soldier = soldier;
                produce(soldier);
            }
        }
    }

    /**
     * 按 maxSoldiers 惰性建槽（子类覆写 maxSoldiers 后仍正确）。
     * 初始错峰：第 i 个槽位计时 spawnIntervalMillis * i（即 0 / 3s / 6s …）。
     */
    private void ensureSlots() {
        while (slots.size() < maxSoldiers) {
            SoldierSlot slot = new SoldierSlot();
            slot.respawnTimer = (int) spawnIntervalMillis * slots.size();
            slots.add(slot);
        }
        while (slots.size() > maxSoldiers) {
            slots.remove(slots.size() - 1);
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
