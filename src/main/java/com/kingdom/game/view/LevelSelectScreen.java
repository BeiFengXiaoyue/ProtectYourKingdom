package com.kingdom.game.view;

import com.kingdom.game.controller.IGameStateReader;
import com.kingdom.game.controller.ILevelSwitcher;
import com.kingdom.game.controller.LevelInfo;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

/**
 * LevelSelectScreen —— 选关界面（**全屏关卡磁贴网格** → 进入所选关卡）。
 *
 * 版式：顶栏（左标题 + 下分隔线）+ 磁贴网格区 + 提示行，整屏铺满（根 StackPane 由装配层放进
 * 与战场屏同尺寸的场景，故菜单屏天然用满 700×636）。网格区 `VBox.setVgrow(ALWAYS)` 吃掉
 * 剩余高度——**关卡再多也只在网格区内竖向滚动**，界面与窗口尺寸恒定（若让卡片随关卡数长高，
 * 根 StackPane 的 pref 会被撑大，窗口跟着变大）。
 *
 * 数据与动作**只走接口**（《多关卡与运行期切图-接口规范》§3.5-1）：查询用
 * {@link IGameStateReader#getLevels()}，切关用 {@link ILevelSwitcher#switchLevelAt(int)}；
 * 不 import 具体 {@code GameController}，也不直接读 {@code GameConfig} / {@code MapLibrary}。
 *
 * 进入关卡的时序：先切关，成功才回调 {@code onEntered} 交回装配层切屏；切关失败时接口保证
 * **状态零变化**，故本界面停在原地提示、可安全重试。
 *
 * 顶栏右侧的「← 返回」由装配层经 {@link #setOnBack(Runnable)} 二段接线（两个屏幕互相引用，
 * 构造期无法直接互通；未接线时按钮隐藏，不出现死按钮）；本界面只回调，不持有其它屏幕引用、
 * 也不动关卡状态——菜单间导航归装配层。
 *
 * 关卡列表只在 {@link #refresh()} 时取一次——{@code getLevels()} 每次调用都会重新解析
 * {@code maps/index.json}（无缓存），不可每帧调用。
 *
 * 磁贴内容为「大号序号 + 关卡名 + 当前关角标」；缩略图位预留但未做（{@code LevelInfo} 不暴露
 * 底图名，要显示需扩该值对象或按约定读 {@code /maps/<key>/map.png}）。
 */
public class LevelSelectScreen {

    // ===== 版式常量（调参只改这里）=====
    /** 磁贴列数（700px 宽下 3 列最舒展） */
    private static final int COLUMNS = 3;
    /** 磁贴尺寸 px（196 = 留出竖向滚动条宽度：3×196+2×16 = 620 < 可视宽度，滚动条出现也不会回流成 2 列） */
    private static final double TILE_WIDTH = 196;
    private static final double TILE_HEIGHT = 120;
    /** 磁贴间距 px */
    private static final double GAP = 16;
    /** 整屏内边距 px */
    private static final double PADDING = 24;

    // ===== 配色（与 GameView 结束遮罩同一套冷灰蓝）=====
    private static final String COLOR_TILE_BG = "rgba(30,34,40,0.96)";
    private static final String COLOR_TILE_BG_HOVER = "rgba(46,52,60,0.98)";
    private static final String COLOR_BORDER = "#9aa3ad";
    private static final String COLOR_BORDER_HOVER = "#c9d2da";
    private static final String COLOR_TEXT = "#dddddd";
    private static final String COLOR_GOLD = "#ffd700";

    private final IGameStateReader stateReader;
    private final ILevelSwitcher levelSwitcher;
    private final Runnable onEntered;

    private final StackPane root;
    private final TilePane grid = new TilePane();
    private final ScrollPane scroller;
    private final Label hintLabel = new Label();
    /** 顶栏「← 返回」：由 {@link #setOnBack(Runnable)} 接线后才显示 */
    private final Button backButton = new Button("← 返回");
    /** 返回动作（装配层注入）；null = 未接线 */
    private Runnable onBack;

    public LevelSelectScreen(IGameStateReader stateReader, ILevelSwitcher levelSwitcher, Runnable onEntered) {
        this.stateReader = stateReader;
        this.levelSwitcher = levelSwitcher;
        this.onEntered = onEntered;

        // ---- 顶栏：左标题 + 右侧「← 返回」 + 底部一条分隔线 ----
        Label title = new Label("选择关卡");
        title.setFont(Font.font(26));
        title.setTextFill(Color.web(COLOR_GOLD));

        backButton.setFont(Font.font(13));
        backButton.setTextFill(Color.web(COLOR_TEXT));
        backButton.setPrefSize(110, 32);
        backButton.setCursor(Cursor.HAND);
        applyBackButtonStyle(false);
        backButton.setOnMouseEntered(e -> applyBackButtonStyle(true));
        backButton.setOnMouseExited(e -> applyBackButtonStyle(false));
        backButton.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });
        setBackButtonVisible(false);   // 未接线前不显示（避免死按钮）

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(title, spacer, backButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));
        header.setStyle("-fx-border-color: transparent transparent " + COLOR_BORDER + " transparent;"
                + "-fx-border-width: 0 0 1 0;");

        // ---- 磁贴网格（3 列；关卡多时由外层 ScrollPane 竖向滚动）----
        grid.setPrefColumns(COLUMNS);
        grid.setHgap(GAP);
        grid.setVgap(GAP);
        grid.setTileAlignment(Pos.TOP_LEFT);
        grid.setAlignment(Pos.TOP_CENTER);

        scroller = new ScrollPane(grid);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // ScrollPane 皮肤默认给 viewport 上底色，与暗色界面不一致 → 先内联透明化；
        // viewport 的实际底色在 refresh() 里再兜一层（部分主题下 -fx-background 不生效）
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroller, Priority.ALWAYS);   // 网格区吃掉剩余高度 → 整屏铺满

        hintLabel.setFont(Font.font(13));
        hintLabel.setTextFill(Color.web("#ff6b5e"));
        hintLabel.setWrapText(true);
        setHint("");

        VBox content = new VBox(12, header, scroller, hintLabel);
        content.setPadding(new Insets(PADDING));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);   // 在 StackPane 里铺满整屏

        root = new StackPane(content);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1b2026, #38414b);");
    }

    public Node getNode() { return root; }

    /**
     * 接线「← 返回」：由装配层在两个屏幕都建好之后调用（二段式，避开两屏互相引用造成的
     * 非法前向引用）。传 null 则按钮隐藏；按钮只回调，不碰关卡状态。
     */
    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
        setBackButtonVisible(onBack != null);
    }

    private void setBackButtonVisible(boolean visible) {
        backButton.setVisible(visible);
        backButton.setManaged(visible);   // 脱管：未接线时不占顶栏右侧空间
    }

    /** 「← 返回」样式：冷灰蓝（与磁贴、结束卡同一套），悬停时底色与描边一起提亮 */
    private void applyBackButtonStyle(boolean hovered) {
        String bg = hovered ? COLOR_TILE_BG_HOVER : COLOR_TILE_BG;
        String border = hovered ? COLOR_BORDER_HOVER : COLOR_BORDER;
        backButton.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 8; -fx-border-width: 1;");
    }

    /** 打开选关界面时调用一次：按当前关卡列表重建磁贴（当前关以金框 + 角标标出） */
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

        int current = stateReader.getCurrentLevelIndex();
        for (LevelInfo info : levels) {
            grid.getChildren().add(buildTile(info, info.getIndex() == current));
        }
    }

    /** 一枚关卡磁贴：大号序号 + 关卡名（+ 当前关角标）；点击进入该关 */
    private StackPane buildTile(LevelInfo info, boolean isCurrent) {
        Label number = new Label(String.valueOf(info.getIndex() + 1));
        number.setFont(Font.font(34));
        number.setTextFill(Color.web(COLOR_GOLD));

        Label name = new Label(info.getName());
        name.setFont(Font.font(14));
        name.setTextFill(Color.web(COLOR_TEXT));
        name.setWrapText(true);
        name.setMaxWidth(TILE_WIDTH - 24);
        name.setAlignment(Pos.CENTER);

        Label currentTag = new Label("当前");
        currentTag.setFont(Font.font(11));
        currentTag.setTextFill(Color.web(COLOR_GOLD));
        currentTag.setVisible(isCurrent);
        currentTag.setManaged(isCurrent);

        VBox box = new VBox(6, number, name, currentTag);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));

        StackPane tile = new StackPane(box);
        tile.setMinSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setPrefSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setMaxSize(TILE_WIDTH, TILE_HEIGHT);
        tile.setCursor(Cursor.HAND);
        applyTileStyle(tile, isCurrent, false);
        tile.setOnMouseEntered(e -> applyTileStyle(tile, isCurrent, true));
        tile.setOnMouseExited(e -> applyTileStyle(tile, isCurrent, false));
        tile.setOnMouseClicked(e -> enter(info));
        return tile;
    }

    /** 磁贴样式：当前关与悬停态用金色描边（当前关更粗），悬停时底色略亮 */
    private void applyTileStyle(StackPane tile, boolean isCurrent, boolean hovered) {
        String border = (isCurrent || hovered) ? COLOR_GOLD : COLOR_BORDER;
        int borderWidth = isCurrent ? 2 : 1;
        String bg = hovered ? COLOR_TILE_BG_HOVER : COLOR_TILE_BG;
        tile.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10;"
                + "-fx-border-color: " + border + "; -fx-border-radius: 10;"
                + "-fx-border-width: " + borderWidth + ";");
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
