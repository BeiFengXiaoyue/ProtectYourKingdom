package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ILevelSwitcher;
import com.kingdom.game.controller.LevelInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LevelSelectScreen —— 选关界面（**全屏关卡磁贴网格**，缩略图卡片，点击直接进入）。
 *
 * 版式：顶栏（左标题 + 下分隔线）+ 磁贴网格区 + 提示行，整屏铺满（根 StackPane 由装配层放进
 * 与战场屏同尺寸的场景，故菜单屏天然用满 700×636）。网格区 `VBox.setVgrow(ALWAYS)` 吃掉
 * 剩余高度——**关卡再多也只在网格区内竖向滚动**，界面与窗口尺寸恒定。
 *
 * 磁贴内容为「该关地图缩略图 + 序号 + 关卡名」（缩略图按约定读 /maps/&lt;key&gt;/map.png）。
 * 进入关卡的时序：先切关，成功才回调 {@code onEntered} 交回装配层切屏；切关失败时接口保证
 * **状态零变化**，故本界面停在原地提示、可安全重试。
 *
 * 关卡列表只在 {@link #refresh()} 时取一次——{@code getLevels()} 每次调用都会重新解析
 * {@code maps/index.json}（无缓存），不可每帧调用。
 */
public class LevelSelectScreen {

    // ===== 版式常量（调参只改这里）=====
    /** 磁贴列数（700px 宽下 3 列最舒展） */
    private static final int COLUMNS = 3;
    /** 磁贴尺寸 px（图少时磁贴放大居中，关卡多也不会溢出——网格区可竖向滚动） */
    private static final double TILE_WIDTH = 174;
    private static final double TILE_HEIGHT = 180;
    /** 磁贴间距 px */
    private static final double GAP = 20;
    /** 整屏内边距 px */
    private static final double PADDING = 24;

    // ===== 配色（与 GameView 结束遮罩 / 开始界面同一套）=====
    private static final String COLOR_TILE_BG = "rgba(236,215,160,0.72)";
    private static final String COLOR_TILE_BG_HOVER = "rgba(248,232,180,0.92)";
    private static final String COLOR_BORDER = "#8a6a30";
    private static final String COLOR_GOLD = "#ffd700";

    private final IGameStateReader stateReader;
    private final ILevelSwitcher levelSwitcher;
    private final Runnable onEntered;

    private final StackPane root;
    private final TilePane grid = new TilePane();
    private final ScrollPane scroller;
    private final Label hintLabel = new Label();
    /** 关卡 key → 缩略图缓存 */
    private final Map<String, Image> thumbCache = new HashMap<>();

    public LevelSelectScreen(IGameStateReader stateReader, ILevelSwitcher levelSwitcher, Runnable onEntered) {
        this.stateReader = stateReader;
        this.levelSwitcher = levelSwitcher;
        this.onEntered = onEntered;

        // ---- 顶栏：左对齐标题 + 底部一条分隔线 ----
        Label title = new Label("选择关卡");
        title.setFont(Font.font(26));
        title.setTextFill(Color.web("#5a3a1a"));
        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));
        header.setStyle("-fx-border-color: transparent transparent " + COLOR_BORDER + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        // ---- 磁贴网格（3 列；关卡多时由外层 ScrollPane 竖向滚动）----
        grid.setPrefColumns(COLUMNS);
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        grid.setTileAlignment(Pos.CENTER);
        grid.setAlignment(Pos.CENTER);

        scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // ScrollPane 皮肤默认给 viewport 上底色，与暗色界面不一致 → 先内联透明化；
        // viewport 的实际底色在 refresh() 里再兜一层（部分主题下 -fx-background 不生效）
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroller, Priority.ALWAYS);   // 网格区吃掉剩余高度 → 整屏铺满
        // 网格撑满可视区高度 → 磁贴的居中对齐才能生效（否则内容高=磁贴高，永远顶格）

        hintLabel.setFont(Font.font(13));
        hintLabel.setTextFill(Color.web("#ff6b5e"));
        hintLabel.setWrapText(true);
        setHint("");

        VBox content = new VBox(12, header, scroller, hintLabel);
        content.setPadding(new Insets(PADDING));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);   // 在 StackPane 里铺满整屏

        root = new StackPane(content);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #c9a86a, #a8823f);");

        // ---- 羊皮纸战役地图底图（铺满整屏；缺图回退上面的沙金渐变）----
        try (java.io.InputStream in = LevelSelectScreen.class.getResourceAsStream("/assets/ui_select_bg.png")) {
            if (in != null) {
                Image parchment = new Image(in);
                ImageView bgView = new ImageView(parchment);
                bgView.setPreserveRatio(false);
                bgView.fitWidthProperty().bind(root.widthProperty());
                bgView.fitHeightProperty().bind(root.heightProperty());
                root.getChildren().add(0, bgView);   // 垫底，内容板叠其上
            }
        } catch (Exception ignored) {
        }
    }

    public Node getNode() { return root; }

    /** 打开选关界面时调用一次：按当前关卡列表重建磁贴（缩略图 + 序号 + 关名） */
    public void refresh() {
        grid.getChildren().clear();
        setHint("");

        // viewport 底色兜底：节点此时已挂在场景里，lookup 可用；取不到就跳过，只影响背景色
        Node viewport = scroller.lookup(".viewport");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: transparent;");
        }

        List<LevelInfo> levels;
        try {
            levels = stateReader.getLevels();
        } catch (RuntimeException ex) {
            // maps/index.json 语法非法时 MiniJson 抛 IllegalArgumentException，而
            // MapLibrary.listMapsFromClasspath 只兜 IOException → 这里必须自行兜底，避免崩窗口
            setHint("关卡列表读取失败：" + ex.getMessage());
            return;
        }
        if (levels == null || levels.isEmpty()) {
            setHint("未找到任何关卡（maps/index.json 缺失或为空）");
            return;
        }

        for (LevelInfo info : levels) {
            grid.getChildren().add(buildTile(info));
        }
    }

    /** 一枚关卡磁贴：地图缩略图（原比例，收在框内）+ 关名；点击进入该关 */
    private StackPane buildTile(LevelInfo info) {
        ImageView thumb = new ImageView(thumbnail(info.getKey()));
        thumb.setFitWidth(150);
        thumb.setPreserveRatio(true);
        thumb.setPreserveRatio(true);
        thumb.setSmooth(true);
        thumb.setMouseTransparent(true);

        Label name = new Label(info.getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 14));
        name.setTextFill(Color.web("#5a3a1a"));
        name.setWrapText(true);
        name.setMaxWidth(TILE_WIDTH - 24);
        name.setAlignment(Pos.CENTER);

        VBox box = new VBox(8, thumb, name);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));

        StackPane tile = new StackPane(box);
        tile.setMinSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setMaxSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setCursor(Cursor.HAND);
        applyTileStyle(tile, false);
        tile.setOnMouseEntered(e -> applyTileStyle(tile, true));
        tile.setOnMouseExited(e -> applyTileStyle(tile, false));
        tile.setOnMouseClicked(e -> enter(info));
        return tile;
    }

    /** 磁贴样式：悬停态用金色描边，悬停时底色略亮 */
    private void applyTileStyle(StackPane tile, boolean hovered) {
        String border = hovered ? COLOR_GOLD : COLOR_BORDER;
        String bg = hovered ? COLOR_TILE_BG_HOVER : COLOR_TILE_BG;
        tile.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 10;"
                + "-fx-border-width: 1;");
    }

    /** 关卡缩略图：读取该图 map.png（classpath），带缓存；缺失回退灰色占位图 */
    private Image thumbnail(String key) {
        Image cached = thumbCache.get(key);
        if (cached != null) return cached;
        Image img = null;
        try (InputStream in = LevelSelectScreen.class.getResourceAsStream("/maps/" + key + "/map.png")) {
            if (in != null) img = new Image(in);
        } catch (Exception ignored) {
        }
        if (img == null) img = placeholder();
        thumbCache.put(key, img);
        return img;
    }

    /** 灰色占位图（126×96） */
    private Image placeholder() {
        javafx.scene.image.WritableImage ph = new javafx.scene.image.WritableImage(126, 96);
        javafx.scene.canvas.Canvas c = new javafx.scene.canvas.Canvas(126, 96);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.web("#4a4a4a"));
        g.fillRect(0, 0, 126, 96);
        g.setFill(Color.web("#777777"));
        g.fillText("No Image", 34, 52);
        javafx.scene.SnapshotParameters sp = new javafx.scene.SnapshotParameters();
        ph = c.snapshot(sp, ph);
        return ph;
    }

    /** 进入所选关卡：切关成功 → 交回装配层切屏；失败 → 留在本界面并提示（失败时状态零变化） */
    private void enter(LevelInfo info) {
        if (levelSwitcher.switchLevelAt(info.getIndex())) {
            onEntered.run();
        } else {
            setHint("关卡数据缺失，无法进入：" + info.getKey());
        }
    }

    /** 提示行：空串时整行隐藏并脱管，避免界面里留一条空行 */
    private void setHint(String text) {
        hintLabel.setText(text);
        boolean show = text != null && !text.isBlank();
        hintLabel.setVisible(show);
        hintLabel.setManaged(show);
    }
}
