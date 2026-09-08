package com.kingdom.game.model.projectile;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;

import java.util.List;

/**
 * Arrow —— 箭矢投射物（追踪模式，箭塔用）。
 * [骨架] 具体实现待填（实体开发 B）。
 *
 * 扩展模式（对照《计划书 v2.0》/《接口契约》）：
 * 继承 Projectile（选追踪构造）→ onHit() / render()；
 * 飞行/锁定目标由基类 update()/fly() 完成。
 */
public class Arrow extends Projectile {

    /**
     * 追踪模式构造：锁定 target，飞行中实时刷新目标坐标。
     *
     * @param target 锁定的敌人（死亡后飞向最后位置）
     * @param damage 命中伤害
     * @param speed  飞行速度 px/s
     */
    public Arrow(double x, double y, Enemy target, int damage, double speed) {
        super(x, y, target, damage, speed);
        // TODO 体型：setWidth/setHeight（细长形）
    }

    @Override
    public void onHit(List<Enemy> enemies) {
        // TODO 命中效果：目标仍存活则 takeDamage(damage)（单体扣血），可经事件槽发命中音效
    }

    @Override
    public void render(GraphicsContext gc) {
        // TODO 贴图优先：drawSprite(gc, AssetKey.ARROW)；
        //     缺失回退：按飞行方向旋转绘制细长箭头（可用 Math.atan2 求角，gc.rotate 绘制）
    }
}
