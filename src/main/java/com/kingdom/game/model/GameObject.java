package com.kingdom.game.model;

import com.kingdom.game.controller.ICombatSound;
import com.kingdom.game.controller.IFloatingTextFx;
import com.kingdom.game.controller.IParticleFx;
import com.kingdom.game.controller.IScreenFx;
import com.kingdom.game.controller.ISelectionFx;
import com.kingdom.game.util.asset.Assets;
import com.kingdom.game.util.asset.SizeTable;
import com.kingdom.game.util.fx.FxNop;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 * GameObject（抽象根类）
 * 所有游戏实体的根，包含位置、渲染、特效注入与生命周期入口。
 *
 * 契约：
 * - 事件槽字段由 GameController 在实体注册/放置时通过 {@link #attachEffects(...)} 注入；
 *   缺省为 util.fx.FxNop 空实现，任何阶段调用都不会 NPE。
 * - destroy() 只表示"实体自身宣告结束"，把实体移出注册表(list)的动作**唯一由 GameController 完成**。
 */
public abstract class GameObject {

    protected double x, y;
    protected double width, height;

    // ===== 事件槽（音效/视觉特效），默认空实现 =====
    protected ICombatSound combatSound = FxNop.NO_COMBAT_SOUND;
    protected IFloatingTextFx fxText = FxNop.NO_FLOATING_TEXT;
    protected IScreenFx fxScreen = FxNop.NO_SCREEN_FX;
    protected IParticleFx fxParticle = FxNop.NO_PARTICLE_FX;
    protected ISelectionFx fxSelection = FxNop.NO_SELECTION_FX;

    /**
     * 常用构造：尺寸由 assets/sizes.json（util.asset.SizeTable）配置驱动，
     * 代码中不再写死任何实体尺寸（见 docs/视觉尺寸配置规范.md）。
     */
    public GameObject(double x, double y) {
        this.x = x;
        this.y = y;
        SizeTable.Entry entry = SizeTable.getInstance().entryFor(getClass());
        this.width = entry.getWidth();
        this.height = entry.getHeight();
    }

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 由 GameController 统一注入事件通道（注册实体/放置塔时调用一次）。
     * 传入 null 表示不替换（保持 FxNop 空实现）。
     */
    public void attachEffects(ICombatSound combatSound,
                              IFloatingTextFx fxText,
                              IScreenFx fxScreen,
                              IParticleFx fxParticle,
                              ISelectionFx fxSelection) {
        if (combatSound != null) this.combatSound = combatSound;
        if (fxText != null) this.fxText = fxText;
        if (fxScreen != null) this.fxScreen = fxScreen;
        if (fxParticle != null) this.fxParticle = fxParticle;
        if (fxSelection != null) this.fxSelection = fxSelection;
    }

    // ===== 生命周期 =====
    public abstract void update();

    public abstract void render(GraphicsContext gc);

    /**
     * 销毁钩子：子类可覆写做额外清理（如停止动画）。
     * 注意：实体从 GameController 注册表移除由 GameController 完成，不在本方法内做。
     */
    public void destroy() { /* 默认空 */ }

    // ===== 贴图辅助（素材插入通道）=====
    /** 按 key 取贴图；不存在返回 null */
    protected Image sprite(AssetKey key) {
        return Assets.get(key);
    }

    /**
     * 以实体中心点(x,y)、宽高(width,height)绘制贴图。
     *
     * @return true 表示贴图存在并已绘制；false 表示未提供贴图（调用方应回退为色块/形状）
     */
    protected boolean drawSprite(GraphicsContext gc, AssetKey key) {
        Image img = Assets.get(key);
        if (img == null) return false;
        gc.drawImage(img, x - width / 2.0, y - height / 2.0, width, height);
        return true;
    }

    // ===== Getter / Setter =====
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
}
