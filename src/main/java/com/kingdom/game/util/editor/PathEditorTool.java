package com.kingdom.game.util.editor;

import com.kingdom.game.util.map.MapLibrary;
import com.kingdom.game.util.map.MapRoute;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PathEditorTool —— 可视化路径标注工具的【启动入口】（本类不继承 Application）。
 *
 * 为什么这样设计：若主类直接继承 Application，在 IntelliJ 普通 Run 配置（JavaFX 走 classpath
 * 而非 module-path）下运行会报“错误: 缺少 JavaFX 运行时组件”。因此把真正窗口逻辑放在嵌套的
 * {@link PathEditorToolApp}，本类 main 只做一次 Application.launch 转发即可直接运行。
 *
 * 使用：把 pom 的 javafx mainClass 改为 com.kingdom.game.util.editor.PathEditorTool 后 mvn javafx:run；
 * IDE 直接运行不可用（JavaFX 模块检查）。
 *
 * 操作：
 * 1. 顶栏选「地图」（maps/index.json 已有地图）或「新建地图…」；
 * 2. 左键标路径点（首点=出生点，末点=终点）；右键删最近点；
 *    Backspace/Delete 撤销最后一个点，Esc 清空，拖拽微调已有；
 * 3. 「保存到该地图」写 当前地图 maps/&lt;key&gt;/path.json；
 *    游戏启动时（GameConfig/MapLibrary 按默认地图）读取，敌人/渲染/出生点随之更新。
 *
 * 坐标语义与游戏一致：像素坐标、路径点为“中心点”。
 */
public final class PathEditorTool {

    private PathEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(PathEditorToolApp.class, args);
    }

    /**
     * 实际的 JavaFX 应用（public static，供 Application.launch 反射实例化）。
     */
    public static final class PathEditorToolApp extends Application {

        private final List<double[]> points = new ArrayList<>();
        private final Canvas canvas = new Canvas();
        private final TextField widthField = new TextField("900");
        private final TextField heightField = new TextField("560");
        private final Label statusLabel = new Label("提示：可选择已有地图或新建地图后标路径。");
        private final ListView<String> pointListView = new ListView<>();

        // 地图分包（maps/index.json + maps/<key>/）
        private final ChoiceBox<String> mapChoice = new ChoiceBox<>();
        private final Map<String, String> mapLabelToKey = new LinkedHashMap<>();
        private String currentMapKey = "default";

        private Image backgroundImage;
        private String backgroundImageName;   // 底图文件名（位于 maps/<key>/ 下）
        private int dragIndex = -1;
        private double canvasWidth = 900;
        private double canvasHeight = 560;

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildToolbar());
            root.setCenter(buildCanvasArea());
            root.setRight(buildSidePanel());

            Scene scene = new Scene(root, 1100, 700);
            scene.setOnKeyPressed(e -> {
                if (scene.getFocusOwner() instanceof TextInputControl) return;
                if (e.getCode() == KeyCode.BACK_SPACE || e.getCode() == KeyCode.DELETE) {
                    removeLastPoint();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    clearPoints();
                }
            });

            stage.setTitle("王国保卫战 · 路径标注工具");
            stage.setScene(scene);
            stage.show();

            setCanvasSize(canvasWidth, canvasHeight);
            installCanvasHandlers();
            redraw();

            mapChoice.setOnAction(e -> onMapSelected());
            initMaps();
        }

        // ================= 地图选择 / 新建 =================
        private void initMaps() {
            populateMaps();
            currentMapKey = MapLibrary.defaultKey();
            if (MapLibrary.exists(currentMapKey)) {
                mapChoice.setValue(MapLibrary.entryLabel(MapLibrary.getEntry(currentMapKey)));
                loadMapByKey(currentMapKey);
            } else {
                statusLabel.setText("暂无可用地图，请「新建地图」或先在 maps/ 建立 index");
            }
        }

        private void populateMaps() {
            mapLabelToKey.clear();
            mapChoice.getItems().clear();
            for (MapLibrary.MapEntry e : MapLibrary.listMaps()) {
                String label = MapLibrary.entryLabel(e);
                mapLabelToKey.put(label, e.getKey());
                mapChoice.getItems().add(label);
            }
        }

        private void onMapSelected() {
            String label = mapChoice.getValue();
            String key = mapLabelToKey.get(label);
            if (key != null && !key.equals(currentMapKey)) {
                currentMapKey = key;
                loadMapByKey(key);
            }
        }

        private void newMap() {
            TextInputDialog k = new TextInputDialog("newmap");
            k.setTitle("新建地图");
            k.setHeaderText("地图 key（目录名，字母数字下划线）");
            k.showAndWait().ifPresent(keyRaw -> {
                String key = keyRaw.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
                if (key.isEmpty()) return;
                TextInputDialog n = new TextInputDialog(key);
                n.setTitle("新建地图");
                n.setHeaderText("显示名称");
                String name = n.showAndWait().orElse(key);
                FileChooser fc = new FileChooser();
                fc.setTitle("选择该地图的底图");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                        "图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
                File img = fc.showOpenDialog(null);
                MapLibrary.MapEntry entry = MapLibrary.createMap(key, name, img);
                if (entry == null) {
                    error("新建失败", "请以仓库根目录运行");
                    return;
                }
                populateMaps();
                mapChoice.setValue(MapLibrary.entryLabel(entry));
                currentMapKey = key;
                loadMapByKey(key);
                statusLabel.setText("已新建地图 " + MapLibrary.entryLabel(entry) + "（可开始标路径）");
            });
        }

        /** 加载某地图：底图 + 已有 path.json（无则空白） */
        private void loadMapByKey(String key) {
            MapLibrary.MapEntry entry = MapLibrary.getEntry(key);
            if (entry == null) {
                statusLabel.setText("地图不存在：" + key);
                return;
            }
            File dir = MapLibrary.mapDir(key);
            File imageFile = dir == null ? null : new File(dir, entry.getImage());
            if (imageFile != null && imageFile.isFile()) {
                Image img = new Image(imageFile.toURI().toString());
                if (!img.isError() && img.getWidth() > 0) {
                    backgroundImage = img;
                    backgroundImageName = entry.getImage();
                    setCanvasSize(img.getWidth(), img.getHeight());
                }
            }
            points.clear();
            MapRoute route = MapLibrary.readPath(key);
            if (route != null) {
                double[] xs = route.getXs();
                double[] ys = route.getYs();
                for (int i = 0; i < xs.length; i++) {
                    points.add(new double[]{xs[i], ys[i]});
                }
                setCanvasSize(route.getWidth(), route.getHeight());
            }
            dragIndex = -1;
            refreshPointList();
            redraw();
            statusLabel.setText("已载入地图 " + MapLibrary.entryLabel(entry));
        }

        // ================= UI 构建 =================
        private HBox buildToolbar() {
            Button newMapBtn = new Button("新建地图…");
            newMapBtn.setOnAction(e -> newMap());

            Button loadImg = new Button("载入图片");
            loadImg.setOnAction(e -> loadImage());

            Button blank = new Button("新建空白画布");
            blank.setOnAction(e -> newBlankCanvas());

            Button undo = new Button("撤销");
            undo.setOnAction(e -> removeLastPoint());

            Button clear = new Button("清空");
            clear.setOnAction(e -> clearPoints());

            Button loadJson = new Button("载入 JSON");
            loadJson.setOnAction(e -> loadJson());

            Button saveGame = new Button("保存到该地图");
            saveGame.setOnAction(e -> saveToGameResources());

            Button saveAs = new Button("另存为 JSON");
            saveAs.setOnAction(e -> saveAsJson());

            mapChoice.setPrefWidth(230);

            HBox bar = new HBox(8, new Label("地图:"), mapChoice, newMapBtn,
                    new SeparatorV(), loadImg, blank, new SeparatorV(), undo, clear,
                    new SeparatorV(), loadJson, saveGame, saveAs);
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private static final class SeparatorV extends Region {
            SeparatorV() { setPrefWidth(2); setStyle("-fx-background-color: #bbb;"); }
        }

        private ScrollPane buildCanvasArea() {
            Group group = new Group(canvas);
            ScrollPane scroll = new ScrollPane(group);
            scroll.setPannable(true);
            scroll.setFitToWidth(false);
            scroll.setFitToHeight(false);
            return scroll;
        }

        private VBox buildSidePanel() {
            Label title = new Label("路线信息");
            title.setStyle("-fx-font-weight: bold;");

            HBox wh = new HBox(6, new Label("宽:"), widthField, new Label("高:"), heightField);
            HBox.setHgrow(widthField, Priority.ALWAYS);
            HBox.setHgrow(heightField, Priority.ALWAYS);

            pointListView.setPrefHeight(360);
            pointListView.setFocusTraversable(false);

            VBox panel = new VBox(8, title, wh, pointListView, statusLabel);
            panel.setPadding(new Insets(10));
            panel.setPrefWidth(250);
            statusLabel.setWrapText(true);
            statusLabel.setStyle("-fx-text-fill: #444;");
            return panel;
        }

        // ================= 画布与绘制 =================
        private void setCanvasSize(double w, double h) {
            canvasWidth = Math.max(16, w);
            canvasHeight = Math.max(16, h);
            canvas.setWidth(canvasWidth);
            canvas.setHeight(canvasHeight);
            widthField.setText(num(canvasWidth));
            heightField.setText(num(canvasHeight));
            redraw();
        }

        private void redraw() {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvasWidth, canvasHeight);

            // 底图
            if (backgroundImage != null) {
                gc.drawImage(backgroundImage, 0, 0, canvasWidth, canvasHeight);
            }

            // 路线（与游戏画法一致：宽 26 深棕）
            if (points.size() >= 2) {
                gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                gc.setStroke(Color.web("#8a5a2b", 0.55));
                gc.setLineWidth(26);
                strokePolyline(gc);
                gc.setStroke(Color.web("#e6c38a", 0.9));   // 中心引导细线
                gc.setLineWidth(3);
                strokePolyline(gc);
            }

            // 各点
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                double r = 6;
                gc.setFill(Color.web("#ff5252"));
                gc.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
                gc.setFill(Color.WHITE);
                gc.setLineWidth(1);
                gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);
                gc.fillText(String.valueOf(i), p[0] + 9, p[1] - 9);
            }

            // 起点 / 终点标记
            if (!points.isEmpty()) {
                double[] start = points.get(0);
                gc.setStroke(Color.web("#2e7d32"));
                gc.setLineWidth(3);
                gc.strokeOval(start[0] - 12, start[1] - 12, 24, 24);

                double[] end = points.get(points.size() - 1);
                gc.setFill(Color.web("#4e342e", 0.85));
                gc.fillRect(end[0] - 13, end[1] - 13, 26, 26);
                gc.setStroke(Color.GOLD);
                gc.setLineWidth(2);
                gc.strokeRect(end[0] - 13, end[1] - 13, 26, 26);
            }

            // 拖拽高亮
            if (dragIndex >= 0 && dragIndex < points.size()) {
                double[] p = points.get(dragIndex);
                gc.setStroke(Color.YELLOW);
                gc.setLineWidth(3);
                gc.strokeOval(p[0] - 10, p[1] - 10, 20, 20);
            }
        }

        private void strokePolyline(GraphicsContext gc) {
            if (points.size() < 2) return;
            gc.beginPath();
            gc.moveTo(points.get(0)[0], points.get(0)[1]);
            for (int i = 1; i < points.size(); i++) {
                gc.lineTo(points.get(i)[0], points.get(i)[1]);
            }
            gc.stroke();
        }

        // ================= 交互 =================
        private void installCanvasHandlers() {
            canvas.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    removeNearest(e.getX(), e.getY());
                    return;
                }
                if (e.getButton() != MouseButton.PRIMARY) return;
                double x = clamp(e.getX());
                double y = clamp(e.getY());
                int hit = hitPoint(x, y, 14);
                if (hit >= 0) {
                    dragIndex = hit;
                } else {
                    addPoint(x, y);
                }
            });
            canvas.setOnMouseDragged(e -> {
                if (dragIndex >= 0 && dragIndex < points.size()) {
                    double[] p = points.get(dragIndex);
                    p[0] = clamp(e.getX());
                    p[1] = clamp(e.getY());
                    redraw();
                    refreshPointList();
                }
            });
            canvas.setOnMouseReleased(e -> dragIndex = -1);
            canvas.setOnContextMenuRequested(e -> e.consume()); // 禁止默认右键菜单
        }

        private double clamp(double v) {
            return Math.max(0, Math.min(v, canvasWidth));
        }

        private int hitPoint(double x, double y, double tol) {
            double best = tol;
            int idx = -1;
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                double d = Math.hypot(p[0] - x, p[1] - y);
                if (d <= best) { best = d; idx = i; }
            }
            return idx;
        }

        private void addPoint(double x, double y) {
            if (!points.isEmpty()) {
                double[] last = points.get(points.size() - 1);
                if (Math.hypot(last[0] - x, last[1] - y) < 2) return;
            }
            points.add(new double[]{x, y});
            statusLabel.setText("已加点 " + (points.size() - 1) + " → (" + num(x) + ", " + num(y) + ")");
            redraw();
            refreshPointList();
        }

        private void removeLastPoint() {
            if (points.isEmpty()) return;
            points.remove(points.size() - 1);
            statusLabel.setText("已撤销最后一个点，剩余 " + points.size() + " 点");
            redraw();
            refreshPointList();
        }

        private void removeNearest(double x, double y) {
            int idx = hitPoint(x, y, 24);
            if (idx < 0) return;
            points.remove(idx);
            dragIndex = -1;
            statusLabel.setText("已删除点 " + idx + "，剩余 " + points.size() + " 点");
            redraw();
            refreshPointList();
        }

        private void clearPoints() {
            points.clear();
            dragIndex = -1;
            statusLabel.setText("已清空全部路径点");
            redraw();
            refreshPointList();
        }

        private void refreshPointList() {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                items.add(i + ": (" + num(p[0]) + ", " + num(p[1]) + ")");
            }
            pointListView.getItems().setAll(items);
        }

        // ================= 载入/新建 =================
        private void loadImage() {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择地图底图");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File file = fc.showOpenDialog(null);
            if (file == null) return;
            Image img = new Image(file.toURI().toString());
            if (img.isError() || img.getWidth() <= 0) {
                statusLabel.setText("图片加载失败: " + file.getName());
                return;
            }
            backgroundImage = img;
            backgroundImageName = file.getName();
            setCanvasSize(img.getWidth(), img.getHeight());
            statusLabel.setText("已载入底图 " + file.getName() + "（" + num(img.getWidth()) + "x"
                    + num(img.getHeight()) + "），开始标点");
            redraw();
        }

        private void newBlankCanvas() {
            double w = readPositive(widthField.getText(), 900);
            double h = readPositive(heightField.getText(), 560);
            backgroundImage = null;
            backgroundImageName = null;
            points.clear();
            dragIndex = -1;
            setCanvasSize(w, h);
            statusLabel.setText("已新建空白画布 " + num(w) + "x" + num(h) + "，开始标点");
            refreshPointList();
        }

        private double readPositive(String text, double fallback) {
            try {
                double v = Double.parseDouble(text.trim());
                if (v >= 16 && v <= 8192) return v;
            } catch (NumberFormatException ignored) {
                // fallthrough
            }
            return fallback;
        }

        // ================= 载入 / 保存 JSON =================
        private void loadJson() {
            FileChooser fc = jsonChooser("载入路线 JSON", false);
            File file = fc.showOpenDialog(null);
            if (file == null) return;
            try {
                MapRoute route = MapRoute.fromJson(Files.readString(file.toPath(), StandardCharsets.UTF_8));
                points.clear();
                double[] xs = route.getXs();
                double[] ys = route.getYs();
                for (int i = 0; i < xs.length; i++) {
                    points.add(new double[]{xs[i], ys[i]});
                }
                dragIndex = -1;
                setCanvasSize(route.getWidth(), route.getHeight());
                statusLabel.setText("已载入 " + points.size() + " 点（如需底图请重新载入图片）");
                refreshPointList();
                redraw();
            } catch (IOException | RuntimeException ex) {
                error("载入失败", ex.getMessage());
            }
        }

        private void saveToGameResources() {
            MapRoute route = toRoute();
            if (route == null) return;
            if (!MapLibrary.writePath(currentMapKey, route)) {
                statusLabel.setText("未找到 src/main/resources（请以仓库根目录运行），已改为“另存为”");
                saveAsJson();
                return;
            }
            File f = MapLibrary.pathFile(currentMapKey);
            statusLabel.setText("已保存到 " + (f == null ? currentMapKey : f.getPath())
                    + "（游戏启动按默认地图读取）");
        }

        private void saveAsJson() {
            FileChooser fc = jsonChooser("另存为路线 JSON", true);
            File file = fc.showSaveDialog(null);
            if (file == null) return;
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            MapRoute route = toRoute();
            if (route == null) return;
            try {
                Files.writeString(file.toPath(), route.toJson(), StandardCharsets.UTF_8);
                statusLabel.setText("已另存为 " + file.getPath());
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        /** 由当前点生成该地图的路由（key 作为 name；image 记录地图底图名） */
        private MapRoute toRoute() {
            double[] xs = new double[points.size()];
            double[] ys = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xs[i] = points.get(i)[0];
                ys[i] = points.get(i)[1];
            }
            if (xs.length < 2) {
                error("导出失败", "至少需要 2 个路径点");
                return null;
            }
            return MapRoute.of(currentMapKey, backgroundImageName,
                    canvasWidth, canvasHeight, xs, ys);
        }

        private FileChooser jsonChooser(String title, boolean save) {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("路线 JSON", "*.json"));
            return fc;
        }

        private void error(String title, String msg) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }

        private static String num(double v) {
            if (v == Math.floor(v) && !Double.isInfinite(v)) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
    }
}
