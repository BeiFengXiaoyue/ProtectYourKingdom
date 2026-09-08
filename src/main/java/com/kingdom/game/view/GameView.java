package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.IFloatingTextFx;
import com.kingdom.game.controller.IGameLoop;
import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.IParticleFx;
import com.kingdom.game.controller.IRenderNotifier;
import com.kingdom.game.controller.IScreenFx;
import com.kingdom.game.controller.ISelectionFx;
import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * GameView —— 主画布（UI 层核心）。
 *
 * 职责（D 开发者）：
 * - 实现 IRenderNotifier：后端每帧结束后回调 requestRender() → 重绘战场；
 * - 实现 4 个视觉特效接口（飘字/屏幕级/粒子/选中高亮），为渲染层预留；
 * - AnimationTimer 每帧驱动 IGameLoop.update(nanoTime)。
 */
public class GameView implements IRenderNotifier,
        IFloatingTextFx, IScreenFx, IParticleFx, ISelectionFx {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Pane root;
    private final GameConfig config;

    private final IGameLoop gameLoop;
    private final IGameStateReader stateReader;

    private final AnimationTimer timer;

    public GameView(IGameLoop gameLoop, IGameStateReader stateReader, GameConfig config) {
        this.gameLoop = gameLoop;
        this.stateReader = stateReader;
        this.config = config;

        this.canvas = new Canvas(config.getViewWidth(), config.getViewHeight());
        this.gc = canvas.getGraphicsContext2D();
        this.root = new Pane(canvas);

        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gameLoop.update(now);
            }
        };

        // 窗口未开始前先画一帧静态地图
        draw();
    }

    public Pane getNode() { return root; }

    public void startLoop() { timer.start(); }

    public void stopLoop() { timer.stop(); }

    /** IRenderNotifier：后端请求重绘 */
    @Override
    public void requestRender() {
        draw();
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 草地背景
        gc.setFill(Color.web(config.getColorBackground()));
        gc.fillRect(0, 0, w, h);

        // 深色路径（粗折线，拐点连线）
        double[] px = config.getPathX();
        double[] py = config.getPathY();
        gc.setStroke(Color.web(config.getColorPath()));
        gc.setLineWidth(26);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.beginPath();
        gc.moveTo(px[0], py[0]);
        for (int i = 1; i < px.length; i++) {
            gc.lineTo(px[i], py[i]);
        }
        gc.stroke();

        // 路径两端：出生口 / 终点(城堡口) 示意
        gc.setFill(Color.web(config.getColorPath()));
        gc.fillOval(px[0] - 16, py[0] - 16, 32, 32);
        gc.setFill(Color.web("#5a3a1a"));
        gc.fillOval(px[px.length - 1] - 20, py[py.length - 1] - 20, 40, 40);

        // 渲染顺序：塔 → 友方 → 敌人 → 投射物
        for (Tower t : stateReader.getTowers()) {
            t.render(gc);
        }
        for (Ally a : stateReader.getAllies()) {
            a.render(gc);
        }
        for (Enemy e : stateReader.getEnemies()) {
            e.render(gc);
        }
        for (Projectile p : stateReader.getProjectiles()) {
            p.render(gc);
        }
    }

    // ================= 视觉特效接口（骨架，D 后续填充实际绘制）=================
    @Override
    public void showFloatingText(double x, double y, String text, String color) {
        // TODO Day3+：维护一个"飘字"列表并在 draw() 中绘制
    }

    @Override
    public void flashScreen(String color, int durationMs) {
        // TODO Day8：全屏闪烁
    }

    @Override
    public void shakeScreen(int durationMs, int intensity) {
        // TODO Day8：屏幕震动
    }

    @Override
    public void showBossWarning() {
        // TODO Day8：Boss 登场预警
    }

    @Override
    public void spawnExplosionParticles(double x, double y, String color, int count) {
        // TODO Day4：爆炸粒子
    }

    @Override
    public void showSelectionHighlight(GameObject target) {
        // TODO Day6：选中高亮圈
    }

    @Override
    public void clearSelectionHighlight() {
        // TODO Day6：取消高亮
    }
}
