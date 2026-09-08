package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.IGameLoop;
import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.IRenderNotifier;
import com.kingdom.game.model.enemy.Enemy;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * GameView —— 主画布（实现 IRenderNotifier）。
 * AnimationTimer 每帧驱动 IGameLoop.update(nanoTime)；
 * 后端每帧结束时回调 requestRender()，本类据此重绘路径与在场敌人。
 * 尺寸/颜色/路径均来自 GameConfig（可开发期修改）。
 */
public class GameView implements IRenderNotifier {

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

        // 在场敌人（各自调用 render）
        for (Enemy e : stateReader.getEnemies()) {
            e.render(gc);
        }
    }
}
