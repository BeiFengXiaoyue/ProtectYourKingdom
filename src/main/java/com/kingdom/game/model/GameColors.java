package com.kingdom.game.model;

/**
 * GameColors（model 层配色常量）
 *
 * <p>实体绘制只用 {@code "#RRGGBB"} 字符串表达颜色（见 {@link IRenderTarget}），
 * 由装配层的绘制目标实现解析。本类收敛<b>跨类重复出现</b>的颜色，避免同一语义的色值在各实体里各写一份；
 * 各实体独有的底色/细节色仍就地写在 {@code render()} 里，不上提为常量。
 *
 * <p>命名口径：常量值一律等价于改造前使用的 JavaFX 颜色，<b>不得改色</b>——
 * 本类只是把颜色"换个地方写"，视觉结果必须与改造前完全一致。
 */
public final class GameColors {

    private GameColors() {
    }

    // ===== 血条（所有活体共用同一口径）=====

    /** 血条底槽 */
    public static final String HP_BAR_BG = "#000000";
    /** 血量前景（等价改造前的 JavaFX {@code Color.LIMEGREEN}） */
    public static final String HP_BAR_FILL = "#32cd32";

    // ===== 等级/精英标记（金）=====

    /** 亮金：等级金星、大师徽记 */
    public static final String GOLD_MARK = "#ffd75e";
    /** 台面金：精英/大师塔台 */
    public static final String GOLD_PLATE = "#d9b74a";
    /** 暗金：金饰描边 */
    public static final String GOLD_DARK = "#8a6d1f";

    // ===== 通用描边 =====

    /** 通用黑色描边 */
    public static final String OUTLINE_BLACK = "#000000";

    // ===== 友方 =====

    /** 友军识别环 */
    public static final String FRIEND_RING = "#66ccff";
    /** 士兵主体蓝 */
    public static final String ALLY_BODY = "#3498db";

    // ===== 三族塔底座（基础/精英/大师同族共用）=====

    /** 箭塔族底座 */
    public static final String TOWER_BASE_ARROW = "#5b4a3a";
    /** 炮塔族底座 */
    public static final String TOWER_BASE_CANNON = "#3a3a3a";
    /** 兵营族底座 */
    public static final String TOWER_BASE_BARRACK = "#62814a";
}
