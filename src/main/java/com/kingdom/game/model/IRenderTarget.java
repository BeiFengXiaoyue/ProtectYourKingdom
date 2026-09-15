package com.kingdom.game.model;

/**
 * IRenderTarget（绘制目标契约，model 层的渲染出口）
 *
 * <p>设计意图：model 层不得依赖 JavaFX。实体只声明"往一个绘制目标上画什么"，
 * 由装配层（view）提供实现（{@code view.FxRenderTarget} 包装 JavaFX {@code GraphicsContext}），
 * 依赖方向为 view → model 接口，形成依赖倒置。
 *
 * <p>契约：
 * <ul>
 *   <li><b>方法名与语义完全对齐 JavaFX {@code GraphicsContext}</b>——便于实现方一对一转发，
 *       也便于把既有绘制代码整段迁移（只需去掉 {@code gc.} 前缀）；
 *       尤其是 {@code fillRect/fillOval/strokeRect/strokeOval} 的 {@code (x, y)} 是<b>左上角</b>，
 *       不是中心点，实现方不得改变该语义。</li>
 *   <li>{@code rotate(degrees)}：单位为度，正值顺时针（与 JavaFX 一致）；
 *       {@code save()/restore()} 是画笔状态栈，必须成对使用。</li>
 *   <li>颜色一律用 {@code "#RRGGBB"} 字符串表达，见 {@link GameColors}；
 *       实现方负责解析为具体颜色对象。</li>
 *   <li>接口无 default 方法（沿例）：任何实现方必须给全，避免"忘了实现却不报错"。</li>
 * </ul>
 */
public interface IRenderTarget {

    // ===== 画笔设置 =====

    /** 设置填充色，形如 {@code "#RRGGBB"} */
    void setFill(String color);

    /** 设置描边色，形如 {@code "#RRGGBB"} */
    void setStroke(String color);

    /** 设置描边线宽（像素） */
    void setLineWidth(double width);

    // ===== 形状绘制（x, y 为左上角）=====

    void fillRect(double x, double y, double w, double h);

    void fillOval(double x, double y, double w, double h);

    void strokeRect(double x, double y, double w, double h);

    void strokeOval(double x, double y, double w, double h);

    /** 填充多边形；{@code xs}/{@code ys} 长度须一致 */
    void fillPolygon(double[] xs, double[] ys);

    // ===== 贴图绘制 =====

    /**
     * 按资源键绘制贴图。
     *
     * @return true 表示贴图存在并已绘制；false 表示素材未提供，
     *         <b>调用方应回退为色块/形状</b>（贴图缺失不是错误，游戏须照常可玩）
     */
    boolean drawAsset(AssetKey key, double x, double y, double w, double h);

    // ===== 画笔状态栈与坐标变换 =====

    void save();

    void restore();

    void translate(double dx, double dy);

    void rotate(double degrees);
}
