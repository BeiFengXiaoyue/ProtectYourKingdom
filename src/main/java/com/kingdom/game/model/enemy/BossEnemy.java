package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * BossEnemy —— 第 10 波首领敌人（血量高、体型偏大），沿预设路径走到终点。
 * 数值（HP/速度/赏金）由 GameController 从 GameConfig 取值后经构造函数传入，类内不写死。
 * 近战手感：高伤（22）中速（1100ms 冷却），本体即威胁。
 *
 * 半血狂暴机制（实体侧已就绪）：
 * - 血量降至 50% 以下时仅触发一次：速度 +50%；
 * - 同时经事件槽发出震地表现（onBossStomp 音效 + 全屏红闪/震屏）；
 * - "眩晕全塔"需要注册表，属 GameController（A）轮询结算职责：实体只把请求记录在 stompRequested 标记中，
 *   由 {@link #consumeStompRequest()} 读取并清除。
 *   ⚠ 现状：GameController 尚未轮询 consumeStompRequest()，"眩晕全塔 3 秒"的全局效果**未生效**
 *   （见《整改方案-文档与代码一致性》§7 P1-3）。
 */
public class BossEnemy extends Enemy {

    /** 半血狂暴：血量低于该比例触发 */
    private static final double ENRAGE_HP_RATIO = 0.5;

    /** 狂暴速度倍率 */
    private static final double ENRAGE_SPEED_MULTIPLIER = 1.5;

    /** 是否已狂暴（全程只触发一次） */
    private boolean enraged = false;

    /** 震地请求标记：由 GameController 轮询 consumeStompRequest() 结算全局效果 */
    private boolean stompRequested = false;

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public BossEnemy(double x, double y, int hp, double speed, int goldReward) {
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, hp, speed, goldReward);
        this.attackDamage = 22;
        this.maxAttackCooldown = 1100;
    }

    @Override
    public void update() {
        super.update();
        // 半血狂暴：仅在存活且未狂暴时检查一次
        if (!enraged && isAlive() && currentHp <= maxHp * ENRAGE_HP_RATIO) {
            triggerEnrage();
        }
    }

    /** 狂暴：提速并发出震地表现；全局结算交给 GameController（经 stompRequested 标记） */
    private void triggerEnrage() {
        enraged = true;
        speed *= ENRAGE_SPEED_MULTIPLIER;
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
