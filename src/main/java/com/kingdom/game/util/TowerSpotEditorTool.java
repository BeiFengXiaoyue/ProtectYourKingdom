package com.kingdom.game.util;

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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
 * TowerSpotEditorTool —— 可视化【塔位手动标点工具】（本类不继承 Application）。
 *
 * 与 PathEditorTool 同一套模式（嵌套 App 解决 JavaFX classpath 运行问题）。
 * 使用：把 pom 的 javafx mainClass 改为 com.kingdom.game.util.TowerSpotEditorTool 后 mvn javafx:run。
 *
 * 操作：
 * 1. 顶栏选「地图」（maps/index.json 已有地图）或「新建地图…」；
 * 2. 左键标塔位点；右键删最近点；Backspace/Delete 撤销最后一点；Esc 清空；拖拽微调；
 * 3. 「保存到该地图」写 当前地图 maps/&lt;key&gt;/spots.json，
 *    游戏启动时（GameConfig/MapLibrary）读取并在画布上渲染塔位标记。
 *
 * 坐标语义与游戏一致：像素坐标、点位为"塔位中心点"。
 */
public final class TowerSpotEditorTool {

    private TowerSpotEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(TowerSpotEditorApp.class, args);
    }

    /**
     * 实际的 JavaFX 应用（public static，供 Application.launch 反射实例化）。
     */
    public static final class TowerSpotEditorApp extends Application {

        private final List<double[]> spots = new ArrayList<>();
        private final Canvas canvas = new Canvas();
        private final Label statusLabel = new Label("提示：选择已有地图或新建地图后标塔位。");
        private final ListView<String> spotListView = new ListView<>();

        // 地图分包（maps/index.json + maps/<key>/）
        private final ChoiceBox<String> mapChoice = new ChoiceBox<>();
        private final Map<String, String> mapLabelToKey = new LinkedHashMap<>();
        private String currentMapKey = "default";

        private Image backgroundImage;
        private double canvasWidth = 700;
        private double canvasHeight = 600;
        private int dragIndex = -1;

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
                    removeLastSpot();
                } else if (e.getCode() == KeyCode.ESCAPE) {
                    clearSpots();
                }
            });

            stage.setTitle("王国保卫战 · 塔位标注工具");
            stage.setScene(scene);
            stage.show();

            installCanvasHandlers();
            mapChoice.setOnAction(e -> onMapSelected());
            initMaps();
            redraw();
        }

        // ================= UI 构建 =================
        private HBox buildToolbar() {
            Button newMapBtn = new Button("新建地图…");
            newMapBtn.setOnAction(e -> newMap());

            Button loadImg = new Button("载入图片");
            loadImg.setOnAction(e -> loadImage());

            Button undo = new Button("撤销");
            undo.setOnAction(e -> removeLastSpot());

            Button clear = new Button("清空");
            clear.setOnAction(e -> clearSpots());

            Button loadJson = new Button("载入 JSON");
            loadJson.setOnAction(e -> loadJson());

            Button saveGame = new Button("保存到该地图");
            saveGame.setOnAction(e -> saveToGameResources());

            Button saveAs = new Button("另存为 JSON");
            saveAs.setOnAction(e -> saveAsJson());

            mapChoice.setPrefWidth(230);

            HBox bar = new HBox(8, new Label("地图:"), mapChoice, newMapBtn,
                    new SeparatorV(), loadImg, new SeparatorV(), undo, clear,
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
            Label title = new Label("塔位列表");
            title.setStyle("-fx-font-weight: bold;");

            spotListView.setPrefHeight(360);
            spotListView.setFocusTraversable(false);

            VBox panel = new VBox(8, title, spotListView, statusLabel);
            panel.setPadding(new Insets(10));
            panel.setPrefWidth(250);
            statusLabel.setWrapText(true);
            statusLabel.setStyle("-fx-text-fill: #444;");
            return panel;
        }

        // ================= 绘制 =================
        private void redraw() {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.DIMGRAY);
            gc.fillRect(0, 0, canvasWidth, canvasHeight);

            if (backgroundImage != null) {
                gc.drawImage(backgroundImage, 0, 0, canvasWidth, canvasHeight);
            }

            // 塔位标记（与游戏内画法一致：黄色圆台 + 深色描边 + 序号）
            for (int i = 0; i < spots.size(); i++) {
                double[] p = spots.get(i);
                double r = 14;
                gc.setFill(Color.web("#f4d03f", 0.55));
                gc.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
                gc.setStroke(Color.web("#7d6608"));
                gc.setLineWidth(2.5);
                gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);
                // 中心小锤位示意
                gc.setFill(Color.web("#7d6608"));
                gc.fillRect(p[0] - 2, p[1] - 7, 4, 14);
                gc.fillRect(p[0] - 6, p[1] - 2, 12, 4);

                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                gc.setFill(Color.BLACK);
                gc.fillText(String.valueOf(i), p[0] + r, p[1] - r + 4);
            }
        }

        // ================= 交互 =================
        private void installCanvasHandlers() {
            canvas.setOnMousePressed(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    removeNearest(e.getX(), e.getY());
                    return;
                }
                if (e.getButton() != MouseButton.PRIMARY) return;
                int hit = hitSpot(e.getX(), e.getY(), 16);
                if (hit >= 0) {
                    dragIndex = hit;
                } else {
                    addSpot(e.getX(), e.getY());
                }
            });
            canvas.setOnMouseDragged(e -> {
                if (dragIndex >= 0 && dragIndex < spots.size()) {
                    double[] p = spots.get(dragIndex);
                    p[0] = clamp(e.getX(), canvasWidth);
                    p[1] = clamp(e.getY(), canvasHeight);
                    redraw();
                    refreshSpotList();
                }
            });
            canvas.setOnMouseReleased(e -> dragIndex = -1);
            canvas.setOnContextMenuRequested(e -> e.consume());
        }

        private double clamp(double v, double max) {
            return Math.max(0, Math.min(v, max));
        }

        private int hitSpot(double x, double y, double tol) {
            double best = tol;
            int idx = -1;
            for (int i = 0; i < spots.size(); i++) {
                double[] p = spots.get(i);
                double d = Math.hypot(p[0] - x, p[1] - y);
                if (d <= best) { best = d; idx = i; }
            }
            return idx;
        }

        private void addSpot(double x, double y) {
            for (double[] p : spots) {
                if (Math.hypot(p[0] - x, p[1] - y) < 8) {
                    statusLabel.setText("该处已有塔位（间距过近），未添加");
                    return;
                }
            }
            spots.add(new double[]{clamp(x, canvasWidth), clamp(y, canvasHeight)});
            statusLabel.setText("已加塔位 " + (spots.size() - 1) + " → (" + num(x) + ", " + num(y) + ")");
            redraw();
            refreshSpotList();
        }

        private void removeLastSpot() {
            if (spots.isEmpty()) return;
            spots.remove(spots.size() - 1);
            statusLabel.setText("已撤销最后一个塔位，剩余 " + spots.size() + " 个");
            redraw();
            refreshSpotList();
        }

        private void removeNearest(double x, double y) {
            int idx = hitSpot(x, y, 24);
            if (idx < 0) return;
            spots.remove(idx);
            dragIndex = -1;
            statusLabel.setText("已删除塔位 " + idx + "，剩余 " + spots.size() + " 个");
            redraw();
            refreshSpotList();
        }

        private void clearSpots() {
            spots.clear();
            dragIndex = -1;
            statusLabel.setText("已清空全部塔位");
            redraw();
            refreshSpotList();
        }

        private void refreshSpotList() {
            List<String> items = new ArrayList<>();
            for (int i = 0; i < spots.size(); i++) {
                double[] p = spots.get(i);
                items.add(i + ": (" + num(p[0]) + ", " + num(p[1]) + ")");
            }
            spotListView.getItems().setAll(items);
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
                statusLabel.setText("已新建地图 " + MapLibrary.entryLabel(entry) + "（可开始标塔位）");
            });
        }

        /** 加载某地图：底图 + 已有 spots.json（无则空白） */
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
                    setCanvasSize(img.getWidth(), img.getHeight());
                }
            }
            spots.clear();
            TowerSpots loaded = MapLibrary.readSpots(key);
            if (loaded != null) {
                double[] xs = loaded.getXs();
                double[] ys = loaded.getYs();
                for (int i = 0; i < xs.length; i++) {
                    spots.add(new double[]{xs[i], ys[i]});
                }
                setCanvasSize(loaded.getWidth(), loaded.getHeight());
            }
            dragIndex = -1;
            refreshSpotList();
            redraw();
            statusLabel.setText("已载入地图 " + MapLibrary.entryLabel(entry)
                    + "（塔位 " + spots.size() + " 个）");
        }

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
            setCanvasSize(img.getWidth(), img.getHeight());
            statusLabel.setText("已载入底图 " + file.getName() + "（" + num(img.getWidth()) + "x"
                    + num(img.getHeight()) + "），左键开始标塔位");
            redraw();
        }

        private void setCanvasSize(double w, double h) {
            canvasWidth = Math.max(16, w);
            canvasHeight = Math.max(16, h);
            canvas.setWidth(canvasWidth);
            canvas.setHeight(canvasHeight);
            redraw();
        }

        // ================= 保存 =================
        private void loadJson() {
            FileChooser fc = jsonChooser("载入塔位 JSON", false);
            File file = fc.showOpenDialog(null);
            if (file == null) return;
            try {
                TowerSpots loaded = TowerSpots.fromJson(
                        Files.readString(file.toPath(), StandardCharsets.UTF_8));
                spots.clear();
                double[] xs = loaded.getXs();
                double[] ys = loaded.getYs();
                for (int i = 0; i < xs.length; i++) {
                    spots.add(new double[]{xs[i], ys[i]});
                }
                dragIndex = -1;
                setCanvasSize(loaded.getWidth(), loaded.getHeight());
                statusLabel.setText("已载入 " + spots.size() + " 个塔位（如需底图请重新载入地图）");
                refreshSpotList();
                redraw();
            } catch (IOException | RuntimeException ex) {
                error("载入失败", ex.getMessage());
            }
        }

        private void saveToGameResources() {
            TowerSpots spotsData = toSpots();
            if (spotsData == null) return;
            if (!MapLibrary.writeSpots(currentMapKey, spotsData)) {
                statusLabel.setText("未找到 src/main/resources（请以仓库根目录运行），已改为\"另存为\"");
                saveAsJson();
                return;
            }
            File f = MapLibrary.spotsFile(currentMapKey);
            statusLabel.setText("已保存到 " + (f == null ? currentMapKey : f.getPath())
                    + "（游戏启动按默认地图读取）");
        }

        private void saveAsJson() {
            FileChooser fc = jsonChooser("另存为塔位 JSON", true);
            File file = fc.showSaveDialog(null);
            if (file == null) return;
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            TowerSpots spotsData = toSpots();
            if (spotsData == null) return;
            try {
                Files.writeString(file.toPath(), spotsData.toJson(), StandardCharsets.UTF_8);
                statusLabel.setText("已另存为 " + file.getPath());
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        /** 由当前点生成该地图的塔位表 */
        private TowerSpots toSpots() {
            double[] xs = new double[spots.size()];
            double[] ys = new double[spots.size()];
            for (int i = 0; i < spots.size(); i++) {
                xs[i] = spots.get(i)[0];
                ys[i] = spots.get(i)[1];
            }
            if (xs.length == 0) {
                error("导出失败", "至少需要 1 个塔位");
                return null;
            }
            return TowerSpots.of(currentMapKey, canvasWidth, canvasHeight, xs, ys);
        }

        private FileChooser jsonChooser(String title, boolean save) {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("塔位 JSON", "*.json"));
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
