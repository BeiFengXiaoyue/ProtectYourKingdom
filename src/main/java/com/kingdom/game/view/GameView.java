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
import javafx.scene.image.Image;
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

    /** 地图底图（resources/maps/<config.mapImageName>），缺失时回退配色画法 */
    private Image mapBackground;

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
        loadMapBackground();
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

    /** 按 config.mapImageName 从 /maps/ 加载底图；缺失/失败保持 null（回退配色渲染） */
    private void loadMapBackground() {
        String name = config.getMapImageName();
        if (name == null || name.isBlank()) return;
        try (java.io.InputStream in = GameView.class.getResourceAsStream("/maps/" + name)) {
            if (in == null) {
                System.err.println("[GameView] 未找到地图底图 /maps/" + name + "，回退配色渲染");
                return;
            }
            mapBackground = new Image(in);
        } catch (Exception e) {
            System.err.println("[GameView] 地图底图加载失败: " + e.getMessage());
        }
    }

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        if (mapBackground != null) {
            // 真实地图底图（含自带道路），不再叠画矢量土路
            gc.drawImage(mapBackground, 0, 0, w, h);
        } else {
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
        }

        // 路径两端：出生口 / 终点(城堡口) 示意
        double[] pathX = config.getPathX();
        double[] pathY = config.getPathY();
        gc.setFill(Color.web(config.getColorPath()));
        gc.fillOval(pathX[0] - 16, pathY[0] - 16, 32, 32);
        gc.setFill(Color.web("#5a3a1a"));
        gc.fillOval(pathX[pathX.length - 1] - 20, pathY[pathY.length - 1] - 20, 40, 40);

        // 塔位标点（TowerSpotEditorTool 手工标注，建塔入口参照物）
        drawTowerSpots();

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

    /** 绘制塔位标点（半透明圆台 + 锤位示意，与 TowerSpotEditorTool 内画法一致） */
    private void drawTowerSpots() {
        var spots = config.getTowerSpots();
        int n = spots.spotCount();
        if (n == 0) return;
        double[] xs = spots.getXs();
        double[] ys = spots.getYs();
        for (int i = 0; i < n; i++) {
            double x = xs[i];
            double y = ys[i];
            double r = 14;
            gc.setFill(Color.web("#f4d03f", 0.55));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setStroke(Color.web("#7d6608"));
            gc.setLineWidth(2.5);
            gc.strokeOval(x - r, y - r, r * 2, r * 2);
            gc.setFill(Color.web("#7d6608"));
            gc.fillRect(x - 2, y - 7, 4, 14);
            gc.fillRect(x - 6, y - 2, 12, 4);
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
