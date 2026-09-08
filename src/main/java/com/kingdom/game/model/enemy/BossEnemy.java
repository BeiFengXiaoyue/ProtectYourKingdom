package com.kingdom.game.model.enemy;

import com.kingdom.game.model.AssetKey;
import javafx.scene.canvas.GraphicsContext;

/**
 * BossEnemy —— 第 10 波首领敌人（血量高、体型最大）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Enemy → 定数值构造 / onDeath() / render()；移动与拦截复用基类模板。
 *
 * 规划机制（Day 7 填）：
 * - 半血狂暴：速度 +50%，触发震地（onBossStomp 音效 + 全屏红闪/震动经事件槽发出）；
 * - "眩晕全塔 3 秒 + 伤害所有活体"需要注册表的世界效果不得在实体内直接操作列表，
 *   建议暴露 stomp 请求标记（如 consumeStompRequest()）由 GameController 轮询结算。
 */
public class BossEnemy extends Enemy {

    /**
     * @param hp         生命值（调用方从 GameConfig 取值传入）
     * @param speed      移动速度 px/s
     * @param goldReward 击杀赏金
     */
    public BossEnemy(double x, double y, int hp, double speed, int goldReward) {
        super(x, y, hp, speed, goldReward);
        // TODO 体型：setWidth/setHeight（全场最大）
        // TODO 近战手感：attackDamage / maxAttackCooldown（比小兵凶）
    }

    @Override
    protected void onDeath() {
        // TODO 死亡表现（大范围粒子/全屏闪光）；胜负结算在 GameController
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.BOSS_ENEMY)，缺失回退暗红色大圆 + 加粗血条
    }
}
