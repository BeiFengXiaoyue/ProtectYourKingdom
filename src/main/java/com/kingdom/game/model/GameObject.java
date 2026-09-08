package com.kingdom.game.model;

import javafx.scene.canvas.GraphicsContext;

/**
 * GameObject（抽象根类）
 * 所有游戏实体的根，仅包含位置与生命周期入口。
 */
public abstract class GameObject {
    protected double x, y;
    protected double width, height;

    public GameObject(double x, double y) {
        this(x, y, 20, 20);
    }

    public GameObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void update();

    public abstract void render(GraphicsContext gc);

    /** 销毁自身，子类覆盖实现自我注销 */
    public void destroy() { /* 默认空 */ }

    // Getter / Setter
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
