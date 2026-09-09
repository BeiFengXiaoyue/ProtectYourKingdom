package com.kingdom.game.model.tower;

import com.kingdom.game.model.AssetKey;
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
 * 生产流程：update() 定时（默认 3 秒/个，上限 3 个）生成 {@link Soldier}，
 * 经自持友方出口 {@link #produce(Ally)} 交给出口；未接线时默认 no-op，产出被丢弃。
 *
 * ⚠ 接口状态：本类自带友方出口（仿 Tower.projectileSink 的写法，但**不改 Tower 基类**）。
 * 装配层（GameController.placeTower）接入 {@link #setAllySink(Consumer)} 后，
 * 士兵才会真正加入战斗；在 A 接线前，单独放置的兵营产出的士兵不会进战场。
 *
 * 数值说明（PRD 给定造价 100、3秒/个、上限 3；士兵属性为占位，待《游戏规则说明书》核对）。
 */
public class Barrack extends Tower {

    /** 建造成本（Main 登记 TowerSpec 时引用，保持一致） */
    public static final int BUILD_COST = 100;

    /** 士兵数量上限 */
    private static final int MAX_SOLDIERS = 3;

    /** 生产间隔（毫秒） */
    private static final long SPAWN_INTERVAL_MILLIS = 3000;

    // ===== 士兵属性（占位可调）=====
    private static final int SOLDIER_HP = 70;
    private static final double SOLDIER_SPEED = 60;
    private static final int SOLDIER_ATTACK = 10;
    private static final int SOLDIER_COOLDOWN = 800;

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
    }

    /** 由装配层（GameController.placeTower）注入友方出口；传入 null 保持当前 */
    public void setAllySink(Consumer<Ally> sink) {
        if (sink != null) this.allySink = sink;
    }

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
        if (spawnCooldown <= 0 && soldiers.size() < MAX_SOLDIERS) {
            Soldier soldier = new Soldier(x, y, SOLDIER_HP, SOLDIER_SPEED,
                    SOLDIER_ATTACK, SOLDIER_COOLDOWN);
            soldiers.add(soldier);
            produce(soldier);
            spawnCooldown = (int) SPAWN_INTERVAL_MILLIS;
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
