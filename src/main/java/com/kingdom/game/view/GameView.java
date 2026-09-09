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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;

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

    // 塔选中 + 详情面板（D：点已占用点位选中塔；出售可用、升级置灰待 A）
    private final TowerDetailPanel detailPanel;
    private Tower selectedTower;         // 当前选中塔（null = 未选中）
    private GameObject highlightTarget;  // 选中高亮目标（ISelectionFx）

    // 结束/胜利覆盖层（内嵌本类构建，不新增类；可见即拦截画布点击）
    private final Runnable onRestart;
    private final StackPane endOverlay;
    private final Label endTitleLabel = new Label();
    private final Label endSubLabel = new Label();
    private final Label endDetailLabel = new Label();
    private final Button endRestartButton = new Button("重新开始");
    private boolean endShown = false;    // 防每帧重复 show 的幂等开关

    private final AnimationTimer timer;

    /** 地图底图（resources/maps/<config.mapImageName>），缺失时回退配色画法 */
    private Image mapBackground;

    public GameView(IGameLoop gameLoop, IGameStateReader stateReader, GameConfig config,
                    ITowerBuilder builder, Runnable onRestart) {
        this.gameLoop = gameLoop;
        this.stateReader = stateReader;
        this.config = config;
        this.builder = builder;
        this.onRestart = onRestart;

        this.canvas = new Canvas(config.getViewWidth(), config.getViewHeight());
        this.gc = canvas.getGraphicsContext2D();

        this.buildMenu = new TowerBuildMenu(stateReader, this::onMenuSelect);
        this.endOverlay = buildEndOverlay();
        this.detailPanel = new TowerDetailPanel(stateReader, builder,
                config.getViewWidth(), config.getViewHeight());

        this.root = new Pane(canvas, buildMenu.getNode(), detailPanel.getNode(), endOverlay);
        canvas.setOnMouseClicked(this::handleCanvasClick);

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

    /** 按 config 的 mapKey + mapImageName 从 /maps/<key>/ 加载底图；缺失/失败保持 null（回退配色渲染） */
    private void loadMapBackground() {
        String key = config.getMapKey();
        String name = config.getMapImageName();
        if (key == null || name == null || name.isBlank()) return;
        String resource = "/maps/" + key + "/" + name;
        try (java.io.InputStream in = GameView.class.getResourceAsStream(resource)) {
            if (in == null) {
                System.err.println("[GameView] 未找到地图底图 " + resource + "，回退配色渲染");
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

        // 塔位标点（唯一来源：当前地图 spots.json，经 util.MapLibrary 加载；已占用点位显示为灰）
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

        // 选中高亮（实体之上、结束遮罩之下）
        drawSelectionHighlight();

        syncEndOverlay();
        syncTowerSelection();
        if (buildMenu.isVisible()) buildMenu.refresh();   // 建塔栏打开时金币实时刷新置灰
    }

    // ================= 塔位交互（唯一来源：当前地图 spots.json，经 util.MapLibrary 加载）=================

    /** 距点击点 < SLOT_CLICK_RADIUS 的最近塔位下标；无塔位/无命中返回 -1 */
    private int findSlotIndex(double x, double y) {
        var spots = config.getTowerSpots();
        int n = spots.spotCount();
        if (n == 0) return -1;
        double[] xs = spots.getXs();
        double[] ys = spots.getYs();
        int best = -1;
        double bestDist = SLOT_CLICK_RADIUS;
        for (int i = 0; i < n; i++) {
            double d = Math.hypot(xs[i] - x, ys[i] - y);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** 塔位中心 SLOT_OCCUPY_RADIUS 内已有塔即视为占用 */
    private boolean isSlotOccupied(int idx) {
        var spots = config.getTowerSpots();
        double[] xs = spots.getXs();
        if (idx < 0 || idx >= xs.length) return false;
        double cx = xs[idx];
        double cy = spots.getYs()[idx];
        for (Tower t : stateReader.getTowers()) {
            if (Math.hypot(t.getX() - cx, t.getY() - cy) < SLOT_OCCUPY_RADIUS) return true;
        }
        return false;
    }

    /** 画布点击：空闲塔位 → 弹建塔目录；已占用塔位 → 选中/取消详情面板；空白 → 取消选中并收起弹窗 */
    private void handleCanvasClick(MouseEvent e) {
        int idx = findSlotIndex(e.getX(), e.getY());
        boolean occupied = idx >= 0 && isSlotOccupied(idx);

        if (idx >= 0 && !occupied) {                     // 空闲塔位 → 建塔目录
            if (buildMenu.isVisible() && idx == pendingSlotIndex) {   // 再点同一点位 → 收起
                hideBuildMenu();
                return;
            }
            clearTowerSelection();
            hideBuildMenu();
            pendingSlotIndex = idx;
            var spots = config.getTowerSpots();
            double[] xs = spots.getXs();
            double[] ys = spots.getYs();
            buildMenu.show(xs[idx], ys[idx], canvas.getWidth(), canvas.getHeight());
            return;
        }

        if (occupied) {                                  // 已占用塔位 → 选中 / 再点取消
            hideBuildMenu();
            Tower t = towerAtSlot(idx);
            if (t == null) return;
            if (t == selectedTower) {
                clearTowerSelection();
            } else {
                selectTower(t);
            }
            return;
        }

        // 空白处：取消选中并收起建塔弹窗
        clearTowerSelection();
        if (buildMenu.isVisible()) hideBuildMenu();
    }

    /** 点位中心 SLOT_OCCUPY_RADIUS 内的塔；无则返回 null */
    private Tower towerAtSlot(int idx) {
        var spots = config.getTowerSpots();
        double[] xs = spots.getXs();
        double[] ys = spots.getYs();
        if (idx < 0 || idx >= xs.length) return null;
        double cx = xs[idx];
        double cy = ys[idx];
        for (Tower t : stateReader.getTowers()) {
            if (Math.hypot(t.getX() - cx, t.getY() - cy) < SLOT_OCCUPY_RADIUS) return t;
        }
        return null;
    }

    /** 选中一座塔：记录 + 高亮 + 详情面板展示 */
    private void selectTower(Tower t) {
        selectedTower = t;
        showSelectionHighlight(t);
        detailPanel.onTowerSelected(t);
    }

    /** 取消选中：清高亮 + 收起详情面板 */
    private void clearTowerSelection() {
        if (selectedTower == null) return;
        selectedTower = null;
        clearSelectionHighlight();
        detailPanel.onTowerDeselected();
    }

    /** 绘制选中高亮圈（ISelectionFx 表现；双层描边环示意塔位） */
    private void drawSelectionHighlight() {
        if (highlightTarget == null) return;
        double cx = highlightTarget.getX();
        double cy = highlightTarget.getY();
        double r = 18;
        gc.setStroke(Color.web("#00e5ff", 0.30));
        gc.setLineWidth(7);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
        gc.setStroke(Color.web("#00e5ff"));
        gc.setLineWidth(2.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
    }

    /** draw() 尾部同步：选中塔已被移除（出售/重开/替换）→ 自动取消；面板可见则刷新 */
    private void syncTowerSelection() {
        if (selectedTower != null && !stateReader.getTowers().contains(selectedTower)) {
            clearTowerSelection();
            return;
        }
        if (detailPanel.isVisible()) detailPanel.refresh();
    }

    /** 目录选型：在弹窗所对应塔位中心落塔；成败/扣钱由后端 placeTower 校验并推送 */
    private void onMenuSelect(TowerSpec spec) {
        int idx = pendingSlotIndex;
        var spots = config.getTowerSpots();
        double[] xs = spots.getXs();
        double[] ys = spots.getYs();
        if (idx < 0 || idx >= xs.length || spec == null) return;
        builder.placeTower(xs[idx], ys[idx], spec.getType());
        pendingSlotIndex = -1;
    }

    private void hideBuildMenu() {
        buildMenu.hide();
        pendingSlotIndex = -1;
    }

    // ================= 结束/胜利覆盖层（内嵌，不新增类）=================

    /** 结束/胜利全画布叠层：半透明遮罩 + 居中卡片（可见即拦截画布点击） */
    private StackPane buildEndOverlay() {
        Rectangle mask = new Rectangle(canvas.getWidth(), canvas.getHeight());
        mask.setFill(Color.rgb(0, 0, 0, 0.55));

        endTitleLabel.setFont(Font.font(36));
        endSubLabel.setFont(Font.font(15));
        endDetailLabel.setFont(Font.font(13));
        endSubLabel.setTextFill(Color.web("#dddddd"));
        endDetailLabel.setTextFill(Color.web("#bbbbbb"));

        endRestartButton.setFont(Font.font(15));
        endRestartButton.setPrefWidth(180);
        endRestartButton.setOnAction(e -> {
            hideEnd();
            onRestart.run();
        });

        VBox card = new VBox(14, endTitleLabel, endSubLabel, endDetailLabel, endRestartButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28, 40, 28, 40));
        card.setStyle("-fx-background-color: rgba(30,34,40,0.96); -fx-background-radius: 12;"
                + "-fx-border-color: #9aa3ad; -fx-border-radius: 12;");

        StackPane overlay = new StackPane(mask, card);
        overlay.setVisible(false);
        return overlay;
    }

    /** 每帧判定：仅在一次终态到达时显示一次；未终态且已隐藏则不动 */
    private void syncEndOverlay() {
        if (endShown) return;
        if (stateReader.isGameOver()) {
            showEnd(false);
        } else if (stateReader.isVictory()) {
            showEnd(true);
        }
    }

    private void showEnd(boolean victory) {
        hideBuildMenu();                       // 清掉可能开着的建塔弹窗
        clearTowerSelection();                 // 结束态清除塔选中
        endTitleLabel.setText(victory ? "胜利！" : "游戏结束");
        endTitleLabel.setTextFill(victory ? Color.web("#ffd700") : Color.web("#ff6b5e"));
        endSubLabel.setText(victory ? "所有波次已击退" : "生命值已耗尽");
        endDetailLabel.setText(victory
                ? "剩余生命 " + stateReader.getCurrentLives()
                : "抵达第 " + stateReader.getCurrentWave() + " / " + stateReader.getTotalWaves() + " 波");
        endOverlay.setVisible(true);
        endShown = true;
    }

    private void hideEnd() {
        endOverlay.setVisible(false);
        endShown = false;
    }

    /** 绘制塔位标点（半透明圆台 + 锤位示意，与 TowerSpotEditorTool 内画法一致；已占用变灰） */
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
            boolean occupied = isSlotOccupied(i);
            gc.setFill(occupied ? Color.web("#9a9a9a", 0.55) : Color.web("#f4d03f", 0.55));
            gc.fillOval(x - r, y - r, r * 2, r * 2);
            gc.setStroke(occupied ? Color.web("#666666") : Color.web("#7d6608"));
            gc.setLineWidth(2.5);
            gc.strokeOval(x - r, y - r, r * 2, r * 2);
            gc.setFill(occupied ? Color.web("#666666") : Color.web("#7d6608"));
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
        highlightTarget = target;
    }

    @Override
    public void clearSelectionHighlight() {
        highlightTarget = null;
    }
}
