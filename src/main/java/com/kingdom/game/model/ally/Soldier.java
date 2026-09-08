package com.kingdom.game.model.ally;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * Soldier —— 近战士兵（兵营生产，拦截敌人）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Ally → move() / findTarget() / onDeath() / render()。
 *
 * 规划机制（Day 4 填）：
 * - move()：目标存活则追击至贴身距离并 tryAttack；目标死亡后待命/回防（出生点锚定）；
 * - findTarget()：返回视野内最近存活敌人（供 GameController 每帧传入敌人列表调用）；
 * - 碰撞拦截由基类 Ally.handleEntityCollision() 完成（锁定目标 + 近战反击）。
 */
public class Soldier extends Ally {

    public Soldier(double x, double y, int hp, double speed,
                   int attackDamage, int attackCooldown) {
        super(x, y, hp, speed, attackDamage, attackCooldown);
        // TODO 体型：setWidth/setHeight
        // TODO 若需要"回防锚点"，在此记录出生点 (x, y) 与索敌/近战距离常量
    }

    @Override
    public Enemy findTarget(List<Enemy> enemies) {
        // TODO 返回视野内最近的存活敌人；无目标返回 null
        return null;
    }

    @Override
    public void move() {
        // TODO 追击当前 target（target 为 null/死亡则清空并待命/回防）
        //     target 由碰撞拦截或外部轮询 findTarget 维护
    }

    @Override
    protected void onDeath() {
        // TODO 死亡表现（粒子）；移除与结算在 GameController
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.SOLDIER)，缺失回退蓝色圆形 + 头顶血条
    }
}
