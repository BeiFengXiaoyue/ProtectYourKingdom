package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.ally.Soldier;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.util.balance.TowerBalance;
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
 * 初始错峰产出（0 / 间隔 / 2×间隔 …）；产出经友方出口交给装配层（出口默认 no-op，未注入时丢弃、不报错）。
 * **产出兵种由 {@link #createSoldier()} 钩子决定**（默认 1 级 Soldier；子类覆写即可换兵种），
 * 生产流程与兵种解耦——新增兵营/兵种只需新增子类重写该钩子，不必改动 {@link #update()}。
 *
 * 升级链（本类为 1 级，可升级到 {@link EliteBarrack}）：
 * 默认 nextLevelSpec 指向 BARRACK_ELITE；架构师 A 可经 {@link #setNextLevelSpec} 注入/替换链。
 * 生产参数（上限/间隔/士兵属性）为**实例字段**，构造期从 config/tower.json 读取（TowerBalance）。
 *
 * 友方出口：本类自带友方出口（仿 Tower.projectileSink 的写法，但**不改 Tower 基类**）。
 * **已接线**：GameController 放置/升级兵营时经 injectTowerChannels 调用 {@link #setAllySink(Consumer)}
 * （注入 {@code this::addAlly}），产出的士兵会进 allies 注册表并注入事件通道。
 */
public class Barrack extends Tower implements ITowerUpgrade {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 100;

    /** 1 级 → 2 级的升级投入（《建筑与怪物机制策划》：100；A 可经 setNextLevelSpec 覆盖） */
    public static final int UPGRADE_COST = 100;

    /** 本塔等级（由类身份决定：每级 = 独立塔类） */
    public static final int LEVEL = 1;

    @Override
    public int getLevel() { return LEVEL; }

    /** 下一级塔目录条目（默认指向 2 级精英兵营；null = 满级） */
    protected TowerSpec nextLevelSpec;

    // ===== 生产与士兵参数（实例字段，构造期从 tower.json 读取；2/3 级子类可覆写）=====
    protected int maxSoldiers = 2;
    protected long spawnIntervalMillis = 5000;
    protected int soldierHp = 50;
    protected double soldierSpeed = 40;
    protected int soldierAttack = 8;
    protected int soldierCooldown = 800;

    /** 友方出口（默认 no-op：未被装配层注入时产出被丢弃、不报错；GameController 放置兵营时已注入） */
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
        // 兵营不直接攻击：射程/冷却/伤害均为 0
        this.attackRange = 0;
        this.attackCooldown = 0;
        this.baseAttackDamage = 0;
        // 士兵生产参数从 config/tower.json 读取
        this.maxSoldiers = TowerBalance.getInt("barrackL1", "maxSoldiers", 2);
        this.spawnIntervalMillis = (long) TowerBalance.getInt("barrackL1", "spawnIntervalMs", 5000);
        this.soldierHp = TowerBalance.getInt("barrackL1", "soldierHp", 50);
        this.soldierSpeed = TowerBalance.getDouble("barrackL1", "soldierSpeed", 40);
        this.soldierAttack = TowerBalance.getInt("barrackL1", "soldierAttack", 11);
        this.soldierCooldown = TowerBalance.getInt("barrackL1", "soldierCooldown", 800);
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

    /**
     * 士兵工厂钩子：**产出兵种由本方法决定**——把「兵营生产流程」与「兵种」解耦。
     *
     * 默认产 1 级 {@link Soldier}；子类只覆写此方法换兵种
     * （{@link EliteBarrack} → EliteSoldier、{@link MasterBarrack} → RoyalSoldier），
     * 数值仍取本兵营的等级字段（soldierHp/soldierSpeed/soldierAttack/soldierCooldown），**不改任何数值**。
     * 新增兵营/兵种时只需新增子类重写本方法，不必改动 {@link #update()} 的生产流程。
     */
    protected Ally createSoldier() {
        return new Soldier(x, y, soldierHp, soldierSpeed, soldierAttack, soldierCooldown);
    }

    /**
     * 当前在役士兵快照（只读，不含已阵亡待剪除的占用者）。
     * 供装配层（GameController）在升级/出售兵营前盘点旧兵；返回副本，改动不影响本兵营槽位。
     */
    public List<Ally> getSoldiers() {
        List<Ally> result = new ArrayList<>(slots.size());
        for (SoldierSlot slot : slots) {
            if (slot.soldier != null && slot.soldier.isAlive()) {
                result.add(slot.soldier);
            }
        }
        return result;
    }

    /**
     * 交出全部在役士兵并清空本兵营的槽位记录（升级/出售兵营时由装配层调用，P1-9）。
     *
     * 后置：返回交出的士兵列表（存活者）；本兵营槽位清零，之后 {@link #update()} 会按
     * 初始错峰节奏（首个计时 0）重新补位 —— 因此调用方应在本兵营被移除/替换前调用，
     * 并自行决定交出的旧兵去留（如从 allies 注册表移除，避免升级后新旧两批兵并存）。
     * 本方法只清兵营侧记录，**不动 allies 注册表**（注册表归 GameController）。
     */
    public List<Ally> releaseSoldiers() {
        List<Ally> handed = getSoldiers();
        slots.clear();
        return handed;
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
                Ally soldier = createSoldier();    // 兵种由钩子决定（默认 Soldier；子类可换）
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
