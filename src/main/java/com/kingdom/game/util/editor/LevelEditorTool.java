package com.kingdom.game.util.editor;

import com.kingdom.game.util.json.MiniJson;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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
import java.util.List;
import java.util.Map;

/**
 * LevelEditorTool —— 关卡可视化设计工具（素材方 E：地图/路线/塔位/关键贴图一站式设计）。
 *
 * 一次设计 = 一份关卡数据（maps/&lt;key&gt;/ 目录）：
 * - map.png    底图（「替换底图…」选择任意图片，保存时自动拷入）；
 * - path.json  路线（首点=敌怪出生点，末点=家）；
 * - spots.json 塔位标注；
 * - 道具贴图：出怪口 / 家 / 塔位基座（选择后拷入 assets/prop_*.png，游戏与预览即时生效）；
 * - 保存新地图时自动注册进 maps/index.json（选关界面即可见）。
 *
 * 三种模式：路线（左键加点/右键删点/拖拽微调）、塔位（同交互）、道具（选贴图）。
 * 波次不在本工具范围（归 F：waves.json 按模板调）。
 */
public final class LevelEditorTool {

    private LevelEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(LevelEditorApp.class, args);
    }

    public static final class LevelEditorApp extends Application {

        private static final String[] PROPS = {"prop_spawn.png", "prop_home.png", "prop_spot.png"};

        private final Canvas canvas = new Canvas(700, 600);
        private final Label statusLabel = new Label("提示：先选地图或新建，再按模式标路线/塔位/换贴图。");
        private final ComboBox<String> mapCombo = new ComboBox<>();
        private final ToggleButton pathMode = new ToggleButton("路线");
        private final ToggleButton spotMode = new ToggleButton("塔位");
        private final ToggleButton propMode = new ToggleButton("道具");

        private Image backgroundImage;
        private File backgroundSource;
        private String mapKey = "default";
        private String mapName = "默认地图";

        private final List<double[]> path = new ArrayList<>();
        private final List<double[]> spots = new ArrayList<>();
        private int dragIndex = -1;
        private int dragSpot = -1;

        private Image propSpawn;
        private Image propHome;
        private Image propSpot;

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildToolbar());
            root.setLeft(buildSidePanel());
            root.setCenter(buildCanvas());

            Scene scene = new Scene(root, 1180, 720);
            scene.setOnKeyPressed(e -> {
                if (scene.getFocusOwner() instanceof TextInputControl) return;
                if ("DELETE".equals(e.getCode().getName())) {
                    if (isPathMode()) removeLast(path);
                    else if (isSpotMode()) removeLast(spots);
                    redraw();
                }
            });

            stage.setTitle("王国保卫战 · 关卡设计工具（地图/路线/塔位/贴图）");
            stage.setScene(scene);
            stage.show();

            loadPropImages();
            refreshMapCombo();
            redraw();
        }

        // ================= 顶栏 =================
        private HBox buildToolbar() {
            Button loadMap = new Button("打开地图");
            loadMap.setOnAction(e -> openMap());

            Button newMap = new Button("新建地图…");
            newMap.setOnAction(e -> newMapDialog());

            Button loadBg = new Button("替换底图…");
            loadBg.setOnAction(e -> loadBackground());

            Button delMap = new Button("删除地图");
            delMap.setOnAction(e -> deleteMap());

            Button renameMap = new Button("重命名…");
            renameMap.setOnAction(e -> renameMapDialog());

            pathMode.setSelected(true);
            pathMode.setOnAction(e -> redraw());
            spotMode.setOnAction(e -> redraw());
            propMode.setOnAction(e -> redraw());

            Button save = new Button("保存关卡");
            save.setOnAction(e -> saveLevel(false));

            Button saveAs = new Button("另存为新地图…");
            saveAs.setOnAction(e -> saveLevel(true));

            HBox bar = new HBox(8,
                    new Label("地图:"), mapCombo, loadMap, newMap, loadBg, delMap, renameMap,
                    new Separator(),
                    pathMode, spotMode, propMode,
                    new Separator(),
                    save, saveAs);
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private VBox buildSidePanel() {
            Label help = new Label("""
                    路线模式：左键加点（首点=出怪口，末点=家）
                    右键删点 / 拖拽微调 / Delete 撤销末点
                    塔位模式：左键加塔位 / 右键删塔位
                    道具模式：选三张贴图（出怪口/家/塔位）

                    保存后重启游戏生效；
                    新地图保存时自动注册选关界面。""");
            help.setWrapText(true);
            statusLabel.setWrapText(true);
            statusLabel.setStyle("-fx-text-fill: #444;");
            VBox box = new VBox(10, new Label("操作说明"), help, propButtons(), statusLabel);
            box.setPadding(new Insets(10));
            box.setPrefWidth(230);
            return box;
        }

        private VBox propButtons() {
            Button spawn = new Button("出怪口贴图…");
            Button home = new Button("家贴图…");
            Button spot = new Button("塔位贴图…");
            spawn.setOnAction(e -> pickProp("prop_spawn.png"));
            home.setOnAction(e -> pickProp("prop_home.png"));
            spot.setOnAction(e -> pickProp("prop_spot.png"));
            HBox row = new HBox(6, spawn, home, spot);
            VBox box = new VBox(6, new Label("关键贴图（拷入 assets/，游戏直接使用）"), row);
            box.setPadding(new Insets(6));
            return box;
        }

        private Group buildCanvas() {
            Group group = new Group(canvas);
            installHandlers();
            return new Group(group);
        }

        // ================= 数据读写 =================
        private File resourcesDir() {
            File dir = new File(System.getProperty("user.dir"));
            for (int i = 0; i < 8 && dir != null; i++) {
                File candidate = new File(dir, "src/main/resources");
                if (candidate.isDirectory()) return candidate;
                dir = dir.getParentFile();
            }
            return null;
        }

        private File mapsRoot() {
            File res = resourcesDir();
            return res == null ? null : new File(res, "maps");
        }

        private File mapDir(String key) {
            File root = mapsRoot();
            return root == null ? null : new File(root, key);
        }

        /** 枚举 maps/ 下含 path.json 的地图目录填入下拉框 */
        private void refreshMapCombo() {
            mapCombo.getItems().clear();
            File root = mapsRoot();
            if (root == null) return;
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                List<String> keys = new ArrayList<>();
                for (File d : dirs) {
                    if (new File(d, "path.json").isFile()) keys.add(d.getName());
                }
                keys.sort(String::compareTo);
                mapCombo.getItems().addAll(keys);
            }
            if (mapKey != null && mapCombo.getItems().contains(mapKey)) mapCombo.setValue(mapKey);
        }

        /** 打开已有地图：读底图 + path + spots */
        private void openMap() {
            String key = mapCombo.getValue();
            if (key == null || key.isBlank()) {
                status("先在下拉框选一个地图");
                return;
            }
            File dir = mapDir(key);
            if (dir == null || !dir.isDirectory()) {
                status("maps/" + key + " 不存在");
                return;
            }
            mapKey = key;
            try {
                File png = new File(dir, "map.png");
                if (png.isFile()) {
                    backgroundImage = new Image(png.toURI().toString());
                    backgroundSource = png;
                    canvas.setWidth(backgroundImage.getWidth());
                    canvas.setHeight(backgroundImage.getHeight());
                }
                path.clear();
                Map<String, Object> root = asMap(Files.readString(new File(dir, "path.json").toPath()));
                for (Object o : (List<?>) root.get("points")) {
                    Map<String, Object> p = castMap(o);
                    path.add(new double[]{toDouble(p.get("x")), toDouble(p.get("y"))});
                }
                spots.clear();
                File sf = new File(dir, "spots.json");
                if (sf.isFile()) {
                    Map<String, Object> sroot = asMap(Files.readString(sf.toPath()));
                    for (Object o : (List<?>) sroot.get("spots")) {
                        Map<String, Object> p = castMap(o);
                        spots.add(new double[]{toDouble(p.get("x")), toDouble(p.get("y"))});
                    }
                }
                status("已载入 " + key + "：路线 " + path.size() + " 点，塔位 " + spots.size() + " 个");
                redraw();
            } catch (IOException | RuntimeException ex) {
                error("载入失败", ex.getMessage());
            }
        }

        /** 新建地图：输入 key，清空画布从零开始 */
        private void newMapDialog() {
            TextInputDialog dlg = new TextInputDialog("new_map");
            dlg.setTitle("新建地图");
            dlg.setHeaderText("输入新地图 key（小写字母/数字/下划线，如 lava_cave）");
            dlg.setContentText("key:");
            dlg.showAndWait().ifPresent(k -> {
                if (!k.matches("[a-z0-9_]+")) {
                    error("key 不合法", "只能小写字母/数字/下划线");
                    return;
                }
                mapKey = k;
                mapName = k;
                backgroundImage = null;
                backgroundSource = null;
                path.clear();
                spots.clear();
                canvas.setWidth(700);
                canvas.setHeight(600);
                status("新地图 " + k + "：请「替换底图…」，然后标路线和塔位，最后保存关卡");
                redraw();
            });
        }

        /** 删除地图：确认后递归删目录 + 从 index.json 移除注册；default 禁删 */
        private void deleteMap() {
            String key = mapCombo.getValue();
            if (key == null || key.isBlank()) {
                status("先在下拉框选要删除的地图");
                return;
            }
            if ("default".equals(key)) {
                error("禁止删除", "默认地图不可删除");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "确定删除地图 " + key + " 吗？\n（地图目录与选关注册将一并移除，不可恢复）",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("删除地图");
            confirm.setHeaderText(null);
            confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(b -> {
                File dir = mapDir(key);
                if (dir != null && dir.exists()) deleteRecursively(dir);
                File root = mapsRoot();
                if (root != null) {
                    File idx = new File(root, "index.json");
                    if (idx.isFile()) {
                        try {
                            String json = Files.readString(idx.toPath(), StandardCharsets.UTF_8);
                            String updated = json.replaceAll("(?m)^\\s*\\{[^\\n]*?\"" + key
                                    + "\"[^\\n]*?\\},?\\n?", "");
                            Files.writeString(idx.toPath(), updated, StandardCharsets.UTF_8);
                        } catch (IOException ignored) {
                        }
                    }
                }
                status("已删除 " + key);
                mapKey = null;
                backgroundImage = null;
                backgroundSource = null;
                path.clear();
                spots.clear();
                refreshMapCombo();
                redraw();
            });
        }

        private boolean deleteRecursively(File f) {
            if (f == null || !f.exists()) return true;
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursively(k);
            return f.delete();
        }

        /** 重命名地图：改 key（目录移动）+ 改选关显示名；index.json 同步更新；default 禁改 */
        private void renameMapDialog() {
            String oldKey = mapCombo.getValue();
            if (oldKey == null || oldKey.isBlank()) {
                status("先在下拉框选要重命名的地图");
                return;
            }
            if ("default".equals(oldKey)) {
                error("禁止重命名", "默认地图不可重命名");
                return;
            }
            TextInputDialog keyDlg = new TextInputDialog(oldKey);
            keyDlg.setTitle("重命名地图");
            keyDlg.setHeaderText("重命名 " + oldKey + "：输入新 key（小写字母/数字/下划线）");
            keyDlg.setContentText("新 key:");
            keyDlg.showAndWait().ifPresent(newKey -> {
                if (!newKey.matches("[a-z0-9_]+") || newKey.equals(oldKey)) {
                    error("key 不合法", "只能小写字母/数字/下划线，且不能与原名相同");
                    return;
                }
                TextInputDialog nameDlg = new TextInputDialog(newKey);
                nameDlg.setTitle("重命名地图");
                nameDlg.setHeaderText("选关界面显示名（可中文，可跳过）");
                nameDlg.setContentText("显示名:");
                String displayName = nameDlg.showAndWait().orElse(newKey);

                File oldDir = mapDir(oldKey);
                File newDir = mapDir(newKey);
                if (newDir.exists()) {
                    error("重命名失败", "maps/" + newKey + " 已存在");
                    return;
                }
                try {
                    if (oldDir != null && oldDir.exists()) {
                        Files.move(oldDir.toPath(), newDir.toPath());
                    }
                    File root = mapsRoot();
                    if (root != null) {
                        File idx = new File(root, "index.json");
                        if (idx.isFile()) {
                            String json = Files.readString(idx.toPath(), StandardCharsets.UTF_8);
                            // 整行替换：同时更新 key 与选关显示名
                            String updated = json.replaceAll(
                                    "(?m)^\\s*\\{[^\\n]*?\"" + oldKey + "\"[^\\n]*?\\},?$",
                                    "    { \"key\": \"" + newKey + "\", \"name\": \"" + displayName
                                            + "\", \"image\": \"map.png\" }");
                            Files.writeString(idx.toPath(), updated, StandardCharsets.UTF_8);
                        }
                    }
                    mapKey = newKey;
                    status("已重命名：" + oldKey + " → " + newKey);
                    refreshMapCombo();
                    redraw();
                } catch (IOException | RuntimeException ex) {
                    error("重命名失败", ex.getMessage());
                }
            });
        }

        /** 替换底图：选图片 → 设为画布背景（保存关卡时拷入地图目录） */
        private void loadBackground() {            FileChooser fc = new FileChooser();
            fc.setTitle("选择底图（建议 700 宽左右的水平图）");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(null);
            if (file == null) return;
            backgroundImage = new Image(file.toURI().toString());
            backgroundSource = file;
            canvas.setWidth(Math.max(16, backgroundImage.getWidth()));
            canvas.setHeight(Math.max(16, backgroundImage.getHeight()));
            status("底图已替换：" + file.getName() + "，记得保存关卡");
            redraw();
        }

        // ================= 交互 =================
        private void installHandlers() {
            canvas.setOnMousePressed(e -> {
                double x = clamp(e.getX(), canvas.getWidth());
                double y = clamp(e.getY(), canvas.getHeight());
                if (isPathMode()) {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        int hit = hitIndex(path, x, y, 16);
                        if (hit >= 0) path.remove(hit);
                        redraw();
                        return;
                    }
                    int hit = hitIndex(path, x, y, 14);
                    if (hit >= 0) dragIndex = hit;
                    else path.add(new double[]{x, y});
                    redraw();
                } else if (isSpotMode()) {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        int hit = hitIndex(spots, x, y, 20);
                        if (hit >= 0) spots.remove(hit);
                        redraw();
                        return;
                    }
                    int hit = hitIndex(spots, x, y, 18);
                    if (hit >= 0) dragSpot = hit;
                    else spots.add(new double[]{x, y});
                    redraw();
                }
            });
            canvas.setOnMouseDragged(e -> {
                double x = clamp(e.getX(), canvas.getWidth());
                double y = clamp(e.getY(), canvas.getHeight());
                if (isPathMode() && dragIndex >= 0 && dragIndex < path.size()) {
                    path.get(dragIndex)[0] = x;
                    path.get(dragIndex)[1] = y;
                    redraw();
                } else if (isSpotMode() && dragSpot >= 0 && dragSpot < spots.size()) {
                    spots.get(dragSpot)[0] = x;
                    spots.get(dragSpot)[1] = y;
                    redraw();
                }
            });
            canvas.setOnMouseReleased(e -> {
                dragIndex = -1;
                dragSpot = -1;
            });
            canvas.setOnContextMenuRequested(e -> e.consume());
        }

        private boolean isPathMode() { return pathMode.isSelected(); }
        private boolean isSpotMode() { return spotMode.isSelected(); }

        private double clamp(double v, double max) { return Math.max(0, Math.min(v, max)); }

        private int hitIndex(List<double[]> list, double x, double y, double tol) {
            int best = -1;
            double bestD = tol;
            for (int i = 0; i < list.size(); i++) {
                double d = Math.hypot(list.get(i)[0] - x, list.get(i)[1] - y);
                if (d < bestD) { bestD = d; best = i; }
            }
            return best;
        }

        private void removeLast(List<double[]> list) {
            if (!list.isEmpty()) list.remove(list.size() - 1);
        }

        // ================= 绘制 =================
        private void redraw() {
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.DIMGRAY);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, canvas.getWidth(), canvas.getHeight());
            }

            // 路线：沙色道路（宽 30）+ 中线
            if (path.size() >= 2) {
                g.setStroke(Color.web("#d9b380", 0.85));
                g.setLineWidth(30);
                g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
                g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
                g.beginPath();
                g.moveTo(path.get(0)[0], path.get(0)[1]);
                for (int i = 1; i < path.size(); i++) g.lineTo(path.get(i)[0], path.get(i)[1]);
                g.stroke();
                g.setStroke(Color.web("#8a6d3b", 0.8));
                g.setLineWidth(3);
                g.beginPath();
                g.moveTo(path.get(0)[0], path.get(0)[1]);
                for (int i = 1; i < path.size(); i++) g.lineTo(path.get(i)[0], path.get(i)[1]);
                g.stroke();
            }

            // 出怪口 / 家 贴图（路径首末点）
            if (!path.isEmpty()) {
                double[] s = path.get(0);
                double[] h = path.get(path.size() - 1);
                if (propSpawn != null) g.drawImage(propSpawn, s[0] - 32, s[1] - 32, 64, 64);
                else marker(g, s, "#7d3c98", "出怪口");
                if (propHome != null) g.drawImage(propHome, h[0] - 38, h[1] - 38, 76, 76);
                else marker(g, h, "#1a5276", "家");
            }

            // 路线点序号
            g.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            for (int i = 0; i < path.size(); i++) {
                double[] p = path.get(i);
                g.setFill(Color.WHITE);
                g.fillText(String.valueOf(i), p[0] + 7, p[1] - 6);
            }

            // 塔位（贴图或黄圈）
            for (double[] p : spots) {
                if (propSpot != null) {
                    g.drawImage(propSpot, p[0] - 20, p[1] - 20, 40, 40);
                } else {
                    g.setFill(Color.web("#f4d03f", 0.75));
                    g.fillOval(p[0] - 14, p[1] - 14, 28, 28);
                    g.setStroke(Color.web("#7d6608"));
                    g.setLineWidth(2);
                    g.strokeOval(p[0] - 14, p[1] - 14, 28, 28);
                }
            }

            // 模式提示
            g.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            g.setFill(Color.web("#ffffff", 0.9));
            if (isPathMode()) g.fillText("路线模式", 8, 18);
            else if (isSpotMode()) g.fillText("塔位模式", 8, 18);
            else if (propMode.isSelected()) g.fillText("道具模式（左侧换贴图）", 8, 18);
        }

        private void marker(GraphicsContext g, double[] p, String color, String text) {
            g.setFill(Color.web(color));
            g.fillOval(p[0] - 16, p[1] - 16, 32, 32);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            g.fillText(text, p[0] - 22, p[1] - 20);
        }

        /** 选择贴图并立即拷入 assets/prop_*（覆盖），刷新预览；游戏端重启生效 */
        private void pickProp(String targetFile) {
            FileChooser fc = new FileChooser();
            fc.setTitle("选择贴图（需透明背景 PNG）");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("贴图", "*.png"));
            File res = resourcesDir();
            if (res != null) fc.setInitialDirectory(new File(res, "assets"));
            File file = fc.showOpenDialog(null);
            if (file == null) return;
            File dst = new File(new File(resourcesDir(), "assets"), targetFile);
            try {
                Files.copy(file.toPath(), dst.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                error("拷贝失败", ex.getMessage());
                return;
            }
            Image img = new Image(dst.toURI().toString());
            if ("prop_spawn.png".equals(targetFile)) propSpawn = img;
            if ("prop_home.png".equals(targetFile)) propHome = img;
            if ("prop_spot.png".equals(targetFile)) propSpot = img;
            status(targetFile + " 已更新（游戏端重启生效）");
            redraw();
        }

        /** 启动时从 assets/ 读取当前三张道具贴图 */
        private void loadPropImages() {
            for (String f : PROPS) {
                try (java.io.InputStream in = LevelEditorTool.class.getResourceAsStream("/assets/" + f)) {
                    if (in == null) continue;
                    Image img = new Image(in);
                    switch (f) {
                        case "prop_spawn.png" -> propSpawn = img;
                        case "prop_home.png" -> propHome = img;
                        case "prop_spot.png" -> propSpot = img;
                        default -> { }
                    }
                } catch (IOException ignored) {
                }
            }
        }

        // ================= 保存 =================
        private void saveLevel(boolean saveAsNew) {
            if (path.size() < 2) {
                error("保存失败", "路线至少需要 2 个点（首点=出怪口，末点=家）");
                return;
            }
            if (saveAsNew) {
                TextInputDialog dlg = new TextInputDialog(mapKey + "_v2");
                dlg.setTitle("另存为新地图");
                dlg.setHeaderText("输入新地图 key（小写字母/数字/下划线）");
                dlg.setContentText("key:");
                dlg.showAndWait().ifPresent(k -> {
                    if (k.matches("[a-z0-9_]+")) {
                        mapKey = k;
                        mapName = k;
                        saveLevel(false);
                    } else {
                        error("key 不合法", "只能小写字母/数字/下划线");
                    }
                });
                return;
            }
            File dir = mapDir(mapKey);
            if (dir == null) {
                error("保存失败", "未找到 src/main/resources/maps（请以仓库根目录运行）");
                return;
            }
            if (!dir.exists() && !dir.mkdirs()) {
                error("保存失败", "无法创建 " + dir);
                return;
            }
            try {
                if (backgroundSource != null) {
                    Files.copy(backgroundSource.toPath(), new File(dir, "map.png").toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(new File(dir, "path.json").toPath(), pathJson(), StandardCharsets.UTF_8);
                Files.writeString(new File(dir, "spots.json").toPath(), spotsJson(), StandardCharsets.UTF_8);
                registerIndex();
                status("已保存 " + mapKey + "（路线 " + path.size() + " 点 / 塔位 " + spots.size()
                        + " 个）——波次请按模板交 F 配置，重启游戏生效");
                refreshMapCombo();
            } catch (IOException | RuntimeException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        private String pathJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"name\": \"").append(mapKey).append("\",\n  \"image\": \"map.png\",\n");
            sb.append("  \"width\": ").append(num(canvas.getWidth())).append(",\n  \"height\": ")
              .append(num(canvas.getHeight())).append(",\n  \"points\": [");
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\n    {\"x\":").append(num(path.get(i)[0]))
                  .append(",\"y\":").append(num(path.get(i)[1])).append("}");
            }
            sb.append("\n  ]\n}\n");
            return sb.toString();
        }

        private String spotsJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"name\": \"").append(mapKey).append("_spots\",\n");
            sb.append("  \"width\": ").append(num(canvas.getWidth())).append(",\n  \"height\": ")
              .append(num(canvas.getHeight())).append(",\n  \"spots\": [");
            for (int i = 0; i < spots.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\n    {\"x\":").append(num(spots.get(i)[0]))
                  .append(",\"y\":").append(num(spots.get(i)[1])).append("}");
            }
            sb.append("\n  ]\n}\n");
            return sb.toString();
        }

        /** maps/index.json 登记本地图（已存在则跳过） */
        private void registerIndex() {
            File root = mapsRoot();
            if (root == null) return;
            File idxFile = new File(root, "index.json");
            if (!idxFile.isFile()) return;
            try {
                String json = Files.readString(idxFile.toPath(), StandardCharsets.UTF_8);
                if (json.contains("\"" + mapKey + "\"")) return;
                String entry = "    { \"key\": \"" + mapKey + "\", \"name\": \"" + mapName
                        + "\", \"image\": \"map.png\" }\n  ";
                String updated = json.replace("  ]\n}", entry + "  ]\n}");
                Files.writeString(idxFile.toPath(), updated, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }

        // ================= JSON 辅助（复用 util.json.MiniJson）=================
        @SuppressWarnings("unchecked")
        private Map<String, Object> asMap(String json) {
            Object root = new MiniJson(json).parse();
            if (!(root instanceof Map)) throw new IllegalArgumentException("JSON 顶层必须是对象");
            return (Map<String, Object>) root;
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> castMap(Object o) {
            return (Map<String, Object>) o;
        }

        private double toDouble(Object v) {
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v instanceof String) {
                try {
                    return Double.parseDouble((String) v);
                } catch (NumberFormatException ignored) {
                }
            }
            return 0;
        }

        // ================= 杂项 =================
        private void status(String s) {
            statusLabel.setText(s);
        }

        private void error(String title, String msg) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }

        private static String num(double v) {
            if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
            return String.valueOf(v);
        }
    }
}
