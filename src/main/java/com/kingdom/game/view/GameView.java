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
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.TowerSpec;
import com.kingdom.game.model.ally.Ally;
import com.kingdom.game.model.enemy.Enemy;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;
import com.kingdom.game.util.anim.UnitAnimator;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    // 塔选中 + 详情面板（D：点已占用点位选中塔；升级/出售均可用；L2→L3 待补 *_MASTER 工厂）
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

    // ================= 飘字（IFloatingTextFx）：时间驱动，绘制时现算、过期自清 =================
    /** 单条飘字寿命（ms）：随进度上浮并淡出 */
    private static final long FLOATING_TEXT_LIFE_MS = 900;
    /** 全程上浮距离（px） */
    private static final double FLOATING_TEXT_RISE_PX = 26;

    /** 单位行为动画叠加层（外部观察 + 叠加绘制；未注册/无帧图时不影响原渲染） */
    private final UnitAnimator animator = new UnitAnimator();

    /** 一条飘字记录：绘制状态（偏移/透明度）按 now−birth 现算，不存可变状态 */
    private static final class FloatingText {
        final double x, y;
        final String text;
        final Color color;
        final long birthNanos;

        FloatingText(double x, double y, String text, Color color, long birthNanos) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.birthNanos = birthNanos;
        }
    }

    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // ================= 爆炸粒子（IParticleFx）：同上，时间驱动，绘制时现算、过期自清 =================
    /** 单颗粒子寿命范围（ms）：随机取值让爆散更自然 */
    private static final long PARTICLE_LIFE_MIN_MS = 320;
    private static final long PARTICLE_LIFE_MAX_MS = 560;
    /** 粒子初速范围（px/s）：自爆心各向同性向外辐射 */
    private static final double PARTICLE_SPEED_MIN = 40;
    private static final double PARTICLE_SPEED_MAX = 130;
    /** 初始半径范围（px） */
    private static final double PARTICLE_RADIUS_MIN = 1.6;
    private static final double PARTICLE_RADIUS_MAX = 3.2;
    /** 生命末期收缩到的半径比例 */
    private static final double PARTICLE_SHRINK_TO = 0.25;
    /** 单次爆炸的粒子数上限（防异常入参刷爆列表） */
    private static final int PARTICLE_COUNT_MAX = 60;

    /** 一颗粒子：位置/速度/寿命在入口随机生成，绘制时按 now−birth 现算，不存可变状态 */
    private static final class Particle {
        final double x, y;        // 爆心（世界坐标）
        final double vx, vy;      // 速度 px/s
        final Color color;
        final double radius;      // 初始半径 px
        final long birthNanos;
        final long lifeNanos;

        Particle(double x, double y, double vx, double vy, Color color,
                 double radius, long birthNanos, long lifeNanos) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.radius = radius;
            this.birthNanos = birthNanos;
            this.lifeNanos = lifeNanos;
        }
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random particleRandom = new Random();

    // ================= 屏幕级特效（IScreenFx）：闪屏/震屏，均为时间驱动 =================
    /** 背景向外多画的边距（px）：震屏平移时不露边 */
    private static final double BACKGROUND_OVERSCAN_PX = 16;
    /** 震屏强度上限（px）：防异常入参把画面推出画布 */
    private static final double SCREEN_SHAKE_MAX_PX = 14;
    /** 震屏抖动频率（Hz）；纵向用 1.7 倍频，避免只沿对角线晃动 */
    private static final double SHAKE_FREQ_HZ = 22;
    /** 闪屏最大不透明度（随进度线性衰减到 0） */
    private static final double SCREEN_FLASH_MAX_ALPHA = 0.45;

    private Color flashTint;              // null = 当前无闪屏
    private long flashStartNanos;
    private long flashDurationNanos;

    private long shakeStartNanos;
    private long shakeDurationNanos;
    private double shakeIntensity;

    // ================= Boss 预警（IScreenFx）：时间驱动横幅，与闪屏/震屏同层 =================
    /** 横幅总时长（ms）：前 12% 淡入 / 保持 / 后 30% 淡出 */
    private static final long BOSS_WARNING_LIFE_MS = 2600;
    /** 横幅文案 */
    private static final String BOSS_WARNING_TEXT = "BOSS 来袭";
    /** 横幅纵向位置（画布高度比例）与字号 */
    private static final double BOSS_WARNING_Y_RATIO = 0.28;
    private static final double BOSS_WARNING_FONT_SIZE = 34;

    private boolean bossWarningActive;      // true = 横幅在显示窗口内
    private long bossWarningStartNanos;

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

    /**
     * 登记单位动画（kind + 单位类名 → 动画表 JSON 资源地址）。
     * 未登记 / JSON 缺失 / 模式无帧 → 该单位保持自身原渲染（回归无损）。
     */
    public void registerUnitAnimation(String kind, String unitClassName, String descriptorResource) {
        animator.register(kind, unitClassName, descriptorResource);
    }

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
        long now = System.nanoTime();

        // 震屏：世界层整体平移绘制，背景外扩 BACKGROUND_OVERSCAN_PX 防露边（屏幕级特效在还原后画）
        gc.save();
        gc.translate(shakeOffsetX(now), shakeOffsetY(now));

        if (mapBackground != null) {
            // 真实地图底图（含自带道路），不再叠画矢量土路
            gc.drawImage(mapBackground, -BACKGROUND_OVERSCAN_PX, -BACKGROUND_OVERSCAN_PX,
                    w + 2 * BACKGROUND_OVERSCAN_PX, h + 2 * BACKGROUND_OVERSCAN_PX);
        } else {
            // 草地背景
            gc.setFill(Color.web(config.getColorBackground()));
            gc.fillRect(-BACKGROUND_OVERSCAN_PX, -BACKGROUND_OVERSCAN_PX,
                    w + 2 * BACKGROUND_OVERSCAN_PX, h + 2 * BACKGROUND_OVERSCAN_PX);

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

        // 塔位标点（唯一来源：当前地图 spots.json，经 util.map.MapLibrary 加载；已占用点位显示为灰）
        drawTowerSpots();

        // 渲染顺序：塔 → 友方 → 敌人 → 投射物
        // 有动画叠加的单位跳过静态渲染（底图+动画帧透明叠加会产生残影）；动画缺失时保持原渲染
        for (Tower t : stateReader.getTowers()) {
            if (!animator.hasOverlay(t)) {
                t.render(gc);
            }
            animator.overlay(gc, t);
        }
        for (Ally a : stateReader.getAllies()) {
            boolean animated = animator.hasOverlay(a);
            if (!animated) {
                a.render(gc);
            }
            animator.overlay(gc, a);
            a.renderPostAnim(gc);   // 压在动画帧之上的单位标记（如精英兵种的金环/盔缨）
            if (animated) {
                drawHpBar(a);   // 静态渲染被跳过时补画血条
            }
        }
        for (Enemy e : stateReader.getEnemies()) {
            boolean animated = animator.hasOverlay(e);
            if (!animated) {
                e.render(gc);
            }
            animator.overlay(gc, e);
            if (animated) {
                drawHpBar(e);   // 静态渲染被跳过时补画血条
            }
        }
        for (Projectile p : stateReader.getProjectiles()) {
            p.render(gc);
        }

        // 爆炸粒子（世界坐标层：实体之上、选中环/飘字之下）
        drawExplosionParticles();

        // 选中高亮（实体之上、结束遮罩之下）
        drawSelectionHighlight();

        // 飘字（世界坐标层：实体之上、结束遮罩之下）
        drawFloatingTexts();

        gc.restore();   // 结束震屏平移

        // 屏幕级特效（在还原后的坐标系绘制，自身不随震屏晃）
        drawScreenFlash(now);
        drawBossWarning(now);

        syncEndOverlay();
        syncTowerSelection();
        if (buildMenu.isVisible()) buildMenu.refresh();   // 建塔栏打开时金币实时刷新置灰
    }

    // ================= 塔位交互（唯一来源：当前地图 spots.json，经 util.map.MapLibrary 加载）=================

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

    /** draw() 尾部同步：选中塔被移除（出售/重开）→ 自动取消；被升级原位替换（同坐标新塔）→ 选中转移 */
    private void syncTowerSelection() {
        if (selectedTower != null && !stateReader.getTowers().contains(selectedTower)) {
            Tower replacement = towerNear(selectedTower.getX(), selectedTower.getY());
            if (replacement != null) {
                selectTower(replacement);   // 升级替换：选中/详情面板/高亮转移到新塔并即时刷新
            } else {
                clearTowerSelection();
            }
            return;
        }
        if (detailPanel.isVisible()) detailPanel.refresh();
    }

    /** 距 (x,y) < SLOT_OCCUPY_RADIUS 的最近塔；无则 null（原位替换的新塔必然距离≈0；建塔间距 40 保证出售后无邻塔误命中） */
    private Tower towerNear(double x, double y) {
        Tower best = null;
        double bestDist = SLOT_OCCUPY_RADIUS;
        for (Tower t : stateReader.getTowers()) {
            double d = Math.hypot(t.getX() - x, t.getY() - y);
            if (d < bestDist) {
                bestDist = d;
                best = t;
            }
        }
        return best;
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
            clearTransientEffects();   // 防上一局的飘字/粒子在重开后残留
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

    /** 清空时间驱动的临时特效（重开时调用；不清则残留至各自过期为止） */
    private void clearTransientEffects() {
        floatingTexts.clear();
        particles.clear();
        flashTint = null;
        shakeDurationNanos = 0;
        bossWarningActive = false;
    }

    /** 绘制塔位标点（半透明圆台 + 锤位示意，与 TowerSpotEditorTool 内画法一致；已占用变灰） */
    /** 头顶血条（样式与各实体 render 内一致）：黑底 + 绿色当前血量；仅动画跳过静态渲染时使用 */
    private void drawHpBar(LivingEntity u) {
        double r = u.getWidth() / 2.0;
        double ratio = u.getMaxHp() > 0 ? Math.max(0, (double) u.getCurrentHp() / u.getMaxHp()) : 0;
        gc.setFill(Color.BLACK);
        gc.fillRect(u.getX() - r, u.getY() - r - 8, u.getWidth(), 4);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(u.getX() - r, u.getY() - r - 8, u.getWidth() * ratio, 4);
    }

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

    // ================= 视觉特效接口（D：飘字/粒子/闪屏/震屏/Boss 预警均已落地）=================
    @Override
    public void showFloatingText(double x, double y, String text, String color) {
        if (text == null || text.isBlank()) return;
        floatingTexts.add(new FloatingText(x, y, text, parseColor(color, Color.WHITE),
                System.nanoTime()));
    }

    /** 飘字绘制：随进度上浮 + 淡出；黑描边保证亮暗底可读；完成后复位文本相关画笔状态 */
    private void drawFloatingTexts() {
        if (floatingTexts.isEmpty()) return;
        long now = System.nanoTime();
        floatingTexts.removeIf(ft -> now - ft.birthNanos >= FLOATING_TEXT_LIFE_MS * 1_000_000L);
        if (floatingTexts.isEmpty()) return;

        gc.setFont(Font.font(null, FontWeight.BOLD, 13));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BASELINE);
        for (FloatingText ft : floatingTexts) {
            double progress = (now - ft.birthNanos) / (double) (FLOATING_TEXT_LIFE_MS * 1_000_000L);
            gc.setGlobalAlpha(1.0 - progress);
            double ty = ft.y - FLOATING_TEXT_RISE_PX * progress;
            gc.setLineWidth(2);
            gc.setStroke(Color.BLACK);
            gc.strokeText(ft.text, ft.x, ty);
            gc.setFill(ft.color);
            gc.fillText(ft.text, ft.x, ty);
        }
        // 复位画笔状态，避免泄漏到后续帧的其他绘制
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    /** 粒子绘制：按进度向外平移（末段减速）+ 收缩 + 淡出；完成后复位 globalAlpha */
    private void drawExplosionParticles() {
        if (particles.isEmpty()) return;
        long now = System.nanoTime();
        particles.removeIf(p -> now - p.birthNanos >= p.lifeNanos);
        if (particles.isEmpty()) return;

        for (Particle p : particles) {
            double progress = (now - p.birthNanos) / (double) p.lifeNanos;
            double ease = 1.0 - (1.0 - progress) * (1.0 - progress);   // 初速快、末段慢
            double lifeSec = p.lifeNanos / 1e9;
            double px = p.x + p.vx * lifeSec * ease;
            double py = p.y + p.vy * lifeSec * ease;
            double r = p.radius * (1.0 - (1.0 - PARTICLE_SHRINK_TO) * progress);
            gc.setGlobalAlpha(1.0 - progress);
            gc.setFill(p.color);
            gc.fillOval(px - r, py - r, r * 2, r * 2);
        }
        gc.setGlobalAlpha(1.0);   // 唯一需复位的状态（fill 泄漏与既有绘制习惯一致）
    }

    /**
     * 颜色串解析：项目命名色优先（比 CSS 同名色更亮，保证亮暗底都可读），
     * 其余交 Color.web（hex 与其它 CSS 色名），非法串回退 fallback 不崩。
     */
    private static Color parseColor(String color, Color fallback) {
        if (color == null || color.isBlank()) return fallback;
        String c = color.trim();
        switch (c.toUpperCase()) {
            case "RED":    return Color.web("#ff4d4d");
            case "GREEN":  return Color.web("#4dff88");
            case "BLUE":   return Color.web("#4da6ff");
            case "GOLD":   return Color.web("#ffd700");
            case "ORANGE": return Color.web("#ffa53c");
            case "YELLOW": return Color.web("#ffee58");
            case "WHITE":  return Color.WHITE;
            default:
                try {
                    return Color.web(c);
                } catch (Exception e) {
                    return fallback;
                }
        }
    }

    @Override
    public void flashScreen(String color, int durationMs) {
        if (durationMs <= 0) return;
        flashTint = parseColor(color, Color.WHITE);   // hex 与命名色均可
        flashStartNanos = System.nanoTime();
        flashDurationNanos = durationMs * 1_000_000L;
    }

    @Override
    public void shakeScreen(int durationMs, int intensity) {
        if (durationMs <= 0 || intensity <= 0) return;
        shakeStartNanos = System.nanoTime();
        shakeDurationNanos = durationMs * 1_000_000L;
        shakeIntensity = Math.min(intensity, SCREEN_SHAKE_MAX_PX);
    }

    /** 震屏横向偏移：满幅、1 倍频 */
    private double shakeOffsetX(long now) { return shakeOffset(now, 1.0, 1.0); }

    /** 震屏纵向偏移：6 成幅、1.7 倍频（与横向错开频率，避免只沿对角线晃动） */
    private double shakeOffsetY(long now) { return shakeOffset(now, 0.6, 1.7); }

    /** 震屏偏移：剩余时间线性衰减 × 正弦抖动；不在震动窗口内（含从未触发）返回 0 */
    private double shakeOffset(long now, double amplitudeScale, double freqScale) {
        if (shakeDurationNanos <= 0) return 0;
        long end = shakeStartNanos + shakeDurationNanos;
        if (now >= end) return 0;
        double remain = (end - now) / (double) shakeDurationNanos;   // 1 → 0
        double phase = (now - shakeStartNanos) / 1e9 * SHAKE_FREQ_HZ * freqScale * 2 * Math.PI;
        return Math.sin(phase) * shakeIntensity * remain * amplitudeScale;
    }

    /** 闪屏：整画布铺一层传入颜色，alpha 随进度线性衰减；过期即清并复位画笔状态 */
    private void drawScreenFlash(long now) {
        if (flashTint == null) return;
        if (now >= flashStartNanos + flashDurationNanos) {
            flashTint = null;
            return;
        }
        double progress = (now - flashStartNanos) / (double) flashDurationNanos;
        gc.setGlobalAlpha(SCREEN_FLASH_MAX_ALPHA * (1.0 - progress));
        gc.setFill(flashTint);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setGlobalAlpha(1.0);
    }

    /**
     * Boss 预警横幅：压暗横带 + 居中红字，包络「前 12% 淡入 / 保持 / 后 30% 淡出」，过期即关。
     * 画在屏幕坐标系（draw() 里 gc.restore() 之后），因此不随震屏晃动。
     */
    private void drawBossWarning(long now) {
        if (!bossWarningActive) return;
        long elapsed = now - bossWarningStartNanos;
        if (elapsed >= BOSS_WARNING_LIFE_MS * 1_000_000L) {
            bossWarningActive = false;   // 过期即清，避免每帧空算
            return;
        }
        double progress = elapsed / (double) (BOSS_WARNING_LIFE_MS * 1_000_000L);
        double alpha = progress < 0.12 ? progress / 0.12
                : (progress > 0.70 ? (1.0 - progress) / 0.30 : 1.0);

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double bandY = h * BOSS_WARNING_Y_RATIO;

        // 压暗横带：保证红字在任意底图上都可读
        gc.setGlobalAlpha(alpha * 0.45);
        gc.setFill(Color.BLACK);
        gc.fillRect(0, bandY - 34, w, 68);

        // 红字 + 黑描边（与飘字同一套写法）
        gc.setGlobalAlpha(alpha);
        gc.setFont(Font.font(null, FontWeight.BOLD, BOSS_WARNING_FONT_SIZE));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.BASELINE);
        gc.setLineWidth(3);
        gc.setStroke(Color.BLACK);
        gc.strokeText(BOSS_WARNING_TEXT, w / 2, bandY + 12);
        gc.setFill(Color.web("#ff3b30"));
        gc.fillText(BOSS_WARNING_TEXT, w / 2, bandY + 12);

        // 复位文本相关画笔状态，避免泄漏到后续帧的其他绘制
        gc.setGlobalAlpha(1.0);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
    }

    /**
     * Boss 登场预警：横幅数秒 + 闪屏/震屏（复用本类已有的屏幕级特效，表现一致）。
     * 调用时机由控制层决定，本方法只负责表现、**不判断波次内容**（是否有 Boss 属控制层口径）。
     */
    @Override
    public void showBossWarning() {
        bossWarningActive = true;
        bossWarningStartNanos = System.nanoTime();
        flashScreen("#ff2222", 300);
        shakeScreen(400, 10);
    }

    @Override
    public void spawnExplosionParticles(double x, double y, String color, int count) {
        if (count <= 0) return;
        int n = Math.min(count, PARTICLE_COUNT_MAX);
        Color base = parseColor(color, Color.ORANGE);   // hex 与命名色（如 "ORANGE"）均可
        long now = System.nanoTime();
        for (int i = 0; i < n; i++) {
            double angle = particleRandom.nextDouble() * Math.PI * 2;   // 各向同性
            double speed = PARTICLE_SPEED_MIN
                    + particleRandom.nextDouble() * (PARTICLE_SPEED_MAX - PARTICLE_SPEED_MIN);
            double lifeMs = PARTICLE_LIFE_MIN_MS
                    + particleRandom.nextDouble() * (PARTICLE_LIFE_MAX_MS - PARTICLE_LIFE_MIN_MS);
            double radius = PARTICLE_RADIUS_MIN
                    + particleRandom.nextDouble() * (PARTICLE_RADIUS_MAX - PARTICLE_RADIUS_MIN);
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed,
                    base, radius, now, (long) (lifeMs * 1_000_000L)));
        }
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
