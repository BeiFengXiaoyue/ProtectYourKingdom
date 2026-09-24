package com.kingdom.game.view;

import com.kingdom.game.model.AssetKey;
import com.kingdom.game.model.IRenderTarget;
import com.kingdom.game.util.asset.Assets;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/**
 * FxRenderTarget —— {@link IRenderTarget} 的 JavaFX 实现（适配器）。
 *
 * <p>把 model 层的绘制调用一对一转发给 JavaFX {@code GraphicsContext}，
 * 是"model 零 JavaFX 依赖"的接线点：model 只认 {@link IRenderTarget}，
 * JavaFX 类型只出现在本类。
 *
 * <p>转发纪律：<b>只翻译类型，不翻译语义</b>——坐标一律原样透传
 * （{@code fillRect/fillOval} 的 {@code (x, y)} 仍是左上角），
 * {@code "#RRGGBB"} 经 {@code Color.web(...)} 解析；不得在此做缩放、翻转或中心点换算。
 */
public final class FxRenderTarget implements IRenderTarget {

    private final GraphicsContext gc;

    public FxRenderTarget(GraphicsContext gc) {
        this.gc = gc;
    }

    // ===== 画笔设置 =====

    @Override
    public void setFill(String color) {
        gc.setFill(Color.web(color));
    }

    @Override
    public void setStroke(String color) {
        gc.setStroke(Color.web(color));
    }

    @Override
    public void setLineWidth(double width) {
        gc.setLineWidth(width);
    }

    // ===== 形状绘制 =====

    @Override
    public void fillRect(double x, double y, double w, double h) {
        gc.fillRect(x, y, w, h);
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        gc.fillOval(x, y, w, h);
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        gc.strokeRect(x, y, w, h);
    }

    @Override
    public void strokeOval(double x, double y, double w, double h) {
        gc.strokeOval(x, y, w, h);
    }

    @Override
    public void fillPolygon(double[] xs, double[] ys) {
        gc.fillPolygon(xs, ys, xs.length);
    }

    // ===== 贴图绘制 =====

    @Override
    public boolean drawAsset(AssetKey key, double x, double y, double w, double h) {
        Image img = Assets.get(key);
        if (img == null) {
            return false;   // 素材未提供：调用方回退色块
        }
        gc.drawImage(img, x, y, w, h);
        return true;
    }

    // ===== 画笔状态栈与坐标变换 =====

    @Override
    public void save() {
        gc.save();
    }

    @Override
    public void restore() {
        gc.restore();
    }

    @Override
    public void translate(double dx, double dy) {
        gc.translate(dx, dy);
    }

    @Override
    public void rotate(double degrees) {
        gc.rotate(degrees);
    }
}
