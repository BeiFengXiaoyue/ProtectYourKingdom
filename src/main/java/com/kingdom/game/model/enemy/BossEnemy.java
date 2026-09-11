package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.util.balance.BalanceTable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * BossEnemy —— 第 10 波首领敌人（血量高、体型偏大），沿预设路径走到终点。
 * 数值（HP/速度/赏金/攻击力/攻击冷却）由调用方取值后经构造函数传入，类内不写死。
 * 近战手感：高伤（22）中速（1100ms 冷却），本体即威胁（默认值见 balance.json `bossAttack*`）。
 *
 * 半血狂暴机制（B-2，2026-09-11 完整落地）：
 * - 血量降至 50% 以下时触发一次（全程仅一次）：速度 +50%；
 * - 触发当刻发一次震地，此后**每 `bossStompIntervalMs`（默认 15000）再震一次**，直到死亡；
 * - 狂暴时攻击冷却按 `bossEnrageAttackCooldownCut`（默认 0.3）削减 → 攻击间隔砍 30%；
 * - 震地表现（onBossStomp 音效 + 全屏红闪/震屏）经事件槽发出；
 * - "眩晕全塔"需要注册表，属 GameController（A）轮询结算职责：实体只把请求记录在 stompRequested 标记中，
 *   由 {@link #consumeStompRequest()} 读取并清除。**A 侧 `settleBossStompRequests()` 已接线**（2026-09-10），
 *   实体每置位一次标记即换来一次全塔眩晕，故本类的周期计时直接决定震地频率。
 *
 * 可调试参数（config/balance.json，改后重启生效，无需改代码）：
 * - `bossStompIntervalMs`：震地间隔 ms（越小震得越频繁；设特别小的值等于全塔长期瘫痪，调试时注意）
 * - `bossEnrageAttackCooldownCut`：攻击冷却削减比例 [0,1)（0.3 = 砍 30%，0 = 不改，越接近 1 攻速越快）
 */
public class BossEnemy extends Enemy {

    /** 半血狂暴：血量低于该比例触发 */
    private static final double ENRAGE_HP_RATIO = 0.5;

    /** 狂暴速度倍率 */
    private static final double ENRAGE_SPEED_MULTIPLIER = 1.5;

    /** 每帧步进 ms（与 LivingEntity/Tower 的固定步长一致） */
    private static final int FRAME_MS = 16;

    /** 是否已狂暴（全程只触发一次） */
    private boolean enraged = false;

    /** 震地请求标记：由 GameController 轮询 consumeStompRequest() 结算全局效果 */
    private boolean stompRequested = false;

    /** 狂暴后距下次震地的剩余 ms（仅 enraged 后有意义） */
    private int stompTimerMs = 0;

    /** 震地间隔 ms：构造期从 balance.json 读入并固化（默认 15000） */
    private final int stompIntervalMs;

    /** 狂暴攻击冷却削减比例：构造期从 balance.json 读入并固化（默认 0.3） */
    private final double enrageAttackCooldownCut;

    /**
     * 老签名（保留重载，B-4）：近战参数从 config/balance.json 取
     * （`bossAttackDamage` / `bossAttackCooldownMs`，缺省回退 22 / 1100）。
     *
     * @param hp         生命值
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public BossEnemy(double x, double y, int hp, double speed, int goldReward) {
        this(x, y, hp, speed, goldReward,
                BalanceTable.runtime().getBossAttackDamage(),
                BalanceTable.runtime().getBossAttackCooldownMs());
    }

    /**
     * B-4 扩参构造：近战参数由调用方（`GameController.spawnEnemy` 经 `GameConfig`）显式注入。
     *
     * 狂暴机制的两个可调参数（震地间隔 / 攻击冷却削减）**不在本签名内**——它们不是
     * `spawnEnemy` 的注入项，而是 Boss 专属机制参数，两处构造均在构造期从
     * `config/balance.json` 读入并固化（改后重启生效）。
     *
     * @param attackDamage      近战攻击力（≥1）
     * @param attackCooldownMs  近战攻击冷却 ms（≥1）；狂暴时按削减比例缩短
     */
    public BossEnemy(double x, double y, int hp, double speed, int goldReward,
                     int attackDamage, int attackCooldownMs) {
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, hp, speed, goldReward);
        this.attackDamage = attackDamage;
        this.maxAttackCooldown = attackCooldownMs;
        BalanceTable bt = BalanceTable.runtime();
        this.stompIntervalMs = bt.getBossStompIntervalMs();
        this.enrageAttackCooldownCut = bt.getBossEnrageAttackCooldownCut();
    }

    @Override
    public void update() {
        super.update();
        // 半血狂暴：仅在存活且未狂暴时检查一次
        if (!enraged && isAlive() && currentHp <= maxHp * ENRAGE_HP_RATIO) {
            triggerEnrage();
        }
        // 狂暴后震地倒计时：每 stompIntervalMs 置位一次请求（由 GameController 轮询结算全塔眩晕）
        if (enraged && isAlive()) {
            stompTimerMs -= FRAME_MS;
            if (stompTimerMs <= 0) {
                // 用累加而非重置，避免 16ms 步长带来的长期漂移
                stompTimerMs += stompIntervalMs;
                if (stompTimerMs <= 0) stompTimerMs = stompIntervalMs;  // 间隔小于一帧时的兜底
                requestStomp();
            }
        }
    }

    /** 狂暴：提速、缩短攻击间隔，并立即震地一次（此后按 stompIntervalMs 周期震地） */
    private void triggerEnrage() {
        enraged = true;
        speed *= ENRAGE_SPEED_MULTIPLIER;
        // 攻击间隔砍掉 enrageAttackCooldownCut 比例（默认 0.3 → 冷却 ×0.7；0 表示不改）
        maxAttackCooldown = (int) Math.round(maxAttackCooldown * (1.0 - enrageAttackCooldownCut));
        stompTimerMs = stompIntervalMs;   // 当刻先震一次，下次在 interval 之后
        requestStomp();
    }

    /**
     * 发起一次震地：置请求标记 + 发出表现（音效/红闪/震屏）。
     * 全局"眩晕全塔"由 GameController 轮询 {@link #consumeStompRequest()} 结算，实体不碰全局列表。
     */
    private void requestStomp() {
        stompRequested = true;
        combatSound.onBossStomp();
        fxScreen.flashScreen("#ff2222", 300);
        fxScreen.shakeScreen(400, 10);
    }

    /** GameController 每帧轮询：true 表示本帧有震地请求待结算（结算后请求被清除） */
    public boolean consumeStompRequest() {
        boolean pending = stompRequested;
        stompRequested = false;
        return pending;
    }

    @Override
    protected void onDeath() {
        // 死亡表现：大范围红色粒子 + 全屏红闪（经事件槽，未接特效层时为空操作）
        // 胜负结算由 GameController 完成，实体不做列表/金币操作。
        fxParticle.spawnExplosionParticles(x, y, "#ff2222", 26);
        fxScreen.flashScreen("#ff0000", 350);
        fxScreen.shakeScreen(500, 12);
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先，未提供素材时回退为暗红色大圆（加粗血条示意首领身份）
        if (!drawSprite(gc, AssetKey.BOSS_ENEMY)) {
            double r = width / 2;
            gc.setFill(Color.web("#8b1a1a"));
            gc.fillOval(x - r, y - r, width, height);
            gc.setStroke(Color.web("#4a0a0a"));
            gc.setLineWidth(2.5);
            gc.strokeOval(x - r, y - r, width, height);
            gc.setFill(Color.web("#5c1111"));   // 中心深色核心
            gc.fillOval(x - r * 0.45, y - r * 0.45, width * 0.45, height * 0.45);
        }

        // 头顶加粗血条
        double r = width / 2;
        double ratio = Math.max(0, (double) currentHp / maxHp);
        gc.setFill(Color.BLACK);
        gc.fillRect(x - r, y - r - 12, width, 6);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x - r, y - r - 12, width * ratio, 6);
    }
}
