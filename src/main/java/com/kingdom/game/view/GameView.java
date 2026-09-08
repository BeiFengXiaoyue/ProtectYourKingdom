package com.kingdom.game.view;

import com.kingdom.game.config.GameConfig;
import com.kingdom.game.controller.IFloatingTextFx;
import com.kingdom.game.controller.IGameLoop;
import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.IParticleFx;
import com.kingdom.game.controller.IRenderNotifier;
import com.kingdom.game.controller.IScreenFx;
import com.kingdom.game.controller.ISelectionFx;
import com.kingdom.game.controller.ITowerBuilder;
import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
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
 * - AnimationTimer 每帧驱动 IGameLoop.update(nanoTime)；
 * - 建塔交互：点空闲可建塔点位弹出锚定目录（TowerBuildMenu），选型后经
 *   ITowerBuilder.placeTower 在该点位中心建塔（校验/提示由后端负责）。
 */
public class GameView implements IRenderNotifier,
        IFloatingTextFx, IScreenFx, IParticleFx, ISelectionFx {

    private static final double SLOT_CLICK_RADIUS = 24;  // 点到点位中心多远算命中
    private static final double SLOT_OCCUPY_RADIUS = 40; // 塔距点位中心多远视为已占用（与后端塔间距一致）

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Pane root;
    private final GameConfig config;

    private final IGameLoop gameLoop;
    private final IGameStateReader stateReader;
    private final ITowerBuilder builder;

    private final TowerBuildMenu buildMenu;
    private int pendingSlotIndex = -1;   // 最近一次弹出目录所对应的点位下标

    private final AnimationTimer timer;

    public GameView(IGameLoop gameLoop, IGameStateReader stateReader, GameConfig config,
                    ITowerBuilder builder) {
        this.gameLoop = gameLoop;
        this.stateReader = stateReader;
        this.config = config;
        this.builder = builder;

        this.canvas = new Canvas(config.getViewWidth(), config.getViewHeight());
        this.gc = canvas.getGraphicsContext2D();

        this.buildMenu = new TowerBuildMenu(stateReader, this::onMenuSelect);

        this.root = new Pane(canvas, buildMenu.getNode());
        canvas.setOnMouseClicked(this::handleCanvasClick);

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

        // 可建塔点位（画在实体之下：空闲亮环，已占用灰环）
        drawBuildSlots();

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

    // ================= 可建塔点位交互（数据驱动，空点位数组时天然安全）=================

    /** 绘制点位：无点位数据时无事可做；已占用以灰态区分，避免误点后白弹菜单 */
    private void drawBuildSlots() {
        double[] xs = config.getBuildSlotX();
        double[] ys = config.getBuildSlotY();
        gc.setLineWidth(3);
        for (int i = 0; i < xs.length; i++) {
            boolean occupied = isSlotOccupied(i);
            gc.setStroke(occupied ? Color.web("#8a8f85") : Color.web("#3f7a2a"));
            gc.strokeOval(xs[i] - 18, ys[i] - 18, 36, 36);
            gc.setFill(occupied ? Color.web("#777777") : Color.web("#ffffff"));
            gc.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
        }
    }

    /** 距点击点 < SLOT_CLICK_RADIUS 的最近点位下标；无点位/无命中返回 -1 */
    private int findSlotIndex(double x, double y) {
        double[] xs = config.getBuildSlotX();
        double[] ys = config.getBuildSlotY();
        int best = -1;
        double bestDist = SLOT_CLICK_RADIUS;
        for (int i = 0; i < xs.length; i++) {
            double d = Math.hypot(xs[i] - x, ys[i] - y);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** 点位中心 SLOT_OCCUPY_RADIUS 内已有塔即视为占用 */
    private boolean isSlotOccupied(int idx) {
        double[] xs = config.getBuildSlotX();
        double[] ys = config.getBuildSlotY();
        if (idx < 0 || idx >= xs.length) return false;
        double cx = xs[idx];
        double cy = ys[idx];
        for (Tower t : stateReader.getTowers()) {
            if (Math.hypot(t.getX() - cx, t.getY() - cy) < SLOT_OCCUPY_RADIUS) return true;
        }
        return false;
    }

    /** 画布点击：空闲点位 → 弹目录；再点同一点位 → 收起；点空白/已占用 → 收起已开弹窗 */
    private void handleCanvasClick(MouseEvent e) {
        int idx = findSlotIndex(e.getX(), e.getY());
        if (buildMenu.isVisible() && idx == pendingSlotIndex) {
            hideBuildMenu();
            return;
        }
        if (idx < 0 || isSlotOccupied(idx)) {
            if (buildMenu.isVisible()) hideBuildMenu();
            return;
        }
        hideBuildMenu();
        pendingSlotIndex = idx;
        double[] xs = config.getBuildSlotX();
        double[] ys = config.getBuildSlotY();
        buildMenu.show(xs[idx], ys[idx], canvas.getWidth(), canvas.getHeight());
    }

    /** 目录选型：在弹窗所对应点位中心落塔；成败/扣钱由后端 placeTower 校验并推送 */
    private void onMenuSelect(TowerSpec spec) {
        int idx = pendingSlotIndex;
        double[] xs = config.getBuildSlotX();
        double[] ys = config.getBuildSlotY();
        if (idx < 0 || idx >= xs.length || spec == null) return;
        builder.placeTower(xs[idx], ys[idx], spec.getType());
        pendingSlotIndex = -1;
    }

    private void hideBuildMenu() {
        buildMenu.hide();
        pendingSlotIndex = -1;
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
