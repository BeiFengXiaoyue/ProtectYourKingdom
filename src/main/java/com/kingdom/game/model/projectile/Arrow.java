package com.kingdom.game.model.projectile;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.enemy.Enemy;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * Arrow —— 箭矢投射物（追踪模式，箭塔用）。
 * 飞行/锁定目标由基类 Projectile 的 update()/fly() 完成；
 * 命中与渲染保持"最小可用"实现（贴图优先、色块回退），素材/特效可后续增强。
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
        // 尺寸（宽/高）由 assets/sizes.json 配置驱动（docs/视觉尺寸配置规范.md）
        super(x, y, target, damage, speed);
    }

    /**
     * 命中：目标仍存活则扣血；事件经基类事件槽发出（未接线时为空操作）。
     *
     * 伤害走**减伤**路径（`takeDamage(damage)`）——箭矢是普通物理伤害，
     * 打重甲敌人时按 `tankPhysicalReduction` 折算。飘字显示**实际扣血**而非面板值。
     */
    @Override
    public void onHit(List<Enemy> enemies) {
        if (targetEnemy != null && targetEnemy.isAlive()) {
            int actual = targetEnemy.takeDamage(damage);
            combatSound.onProjectileHit(this, targetEnemy);
            fxText.showFloatingText(targetEnemy.getX(), targetEnemy.getY() - 24,
                    "-" + actual, "WHITE");
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        // 贴图优先
        if (drawSprite(gc, AssetKey.ARROW)) return;
        // 回退：沿飞行方向画一枚深色小箭
        double angle = Math.atan2(targetY - y, targetX - x);
        gc.save();
        gc.translate(x, y);
        gc.rotate(Math.toDegrees(angle));
        gc.setFill(Color.web("#4a3a2a"));
        gc.fillOval(-3, -3, 6, 6);
        gc.setFill(Color.web("#8f8f8f"));
        gc.fillRect(2, -1.2, 6, 2.4);   // 箭杆
        gc.restore();
    }
}
