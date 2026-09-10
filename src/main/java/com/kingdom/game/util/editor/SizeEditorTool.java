package com.kingdom.game.util.editor;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.util.asset.SizeTable;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * SizeEditorTool —— 实体视觉尺寸可视化设计工具（素材方 E，docs/视觉尺寸配置规范.md §七）。
 *
 * 工程结构仿 AnimEditorTool：本类不继承 Application，main() 转发到嵌套 SizeEditorApp，
 * 规避 IntelliJ classpath 运行时的"缺少 JavaFX 运行时组件"报错。
 *
 * 功能：
 * 1. 反射扫描 com.kingdom.game.model 下全部 GameObject 具体子类（非抽象/非接口/非内部类）；
 * 2. 按子包分组展示（敌人/友方/防御塔/投射物），行内显示当前配置值或"跟随 default"；
 * 3. 宽/高编辑（允许小数，留空=回退 default），即时更新行内显示；
 * 4. 贴图预览：类名 camel→snake 拼 assets/&lt;蛇形&gt;.png，按当前 W×H 绘制；缺图提示不阻塞；
 * 5. 「按原图尺寸」一键读取贴图实际像素填入宽/高；
 * 6. 「保存到游戏资源」合并写回 src/main/resources/assets/sizes.json（重启游戏生效）。
 */
public final class SizeEditorTool {

    private SizeEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(SizeEditorApp.class, args);
    }

    public static final class SizeEditorApp extends Application {

        /** 分组显示顺序 */
        private static final String[] GROUP_ORDER = {"enemy", "ally", "tower", "projectile", "other"};
        private static final String[] GROUP_LABELS = {"敌人", "友方", "防御塔", "投射物", "其他"};

        private final ListView<ClassRow>classList = new ListView<>();
        private final TextField widthField = new TextField();
        private final TextField heightField = new TextField();
        private final Label statusLabel = new Label("提示：选中类后编辑宽/高，留空=跟随 default。");
        private final Label infoLabel = new Label("（未选中类）");
        private final Canvas previewCanvas = new Canvas(240, 240);

        private final SizeTable table = SizeTable.getInstance();
        private ClassRow selected;

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildToolbar());
            root.setLeft(buildClassList());
            root.setCenter(buildEditorPanel());
            root.setBottom(statusBar());

            Scene scene = new Scene(root, 860, 560);
            scene.setOnKeyPressed(e -> {
                if (scene.getFocusOwner() instanceof TextInputControl) return;
                if (e.getCode() == KeyCode.F5) refreshAll();
            });

            stage.setTitle("王国保卫战 · 实体尺寸设计工具");
            stage.setScene(scene);
            stage.show();

            refreshAll();
        }

        // ================= UI 构建 =================
        private HBox buildToolbar() {
            Button reload = new Button("重载 JSON");
            reload.setOnAction(e -> refreshAll());

            Button save = new Button("保存到游戏资源");
            save.setOnAction(e -> saveToRepo());

            Button saveAs = new Button("另存为…");
            saveAs.setOnAction(e -> saveAs());

            HBox bar = new HBox(8, reload, save, saveAs, new Label("（F5 刷新类清单）"));
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private VBox buildClassList() {
            classList.setPrefWidth(300);
            classList.getSelectionModel().selectedItemProperty().addListener((obs, old, cur) -> selectRow(cur));
            VBox box = new VBox(6, new Label("实体类清单（自动枚举）"), classList);
            box.setPadding(new Insets(8));
            VBox.setVgrow(classList, Priority.ALWAYS);
            return box;
        }

        private VBox buildEditorPanel() {
            infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            widthField.setPrefWidth(90);
            heightField.setPrefWidth(90);
            HBox fields = new HBox(10,
                    new Label("宽:"), widthField,
                    new Label("高:"), heightField,
                    originalSizeButton());
            fields.setAlignment(Pos.CENTER_LEFT);
            fields.setPadding(new Insets(4, 0, 4, 0));

            // 宽/高输入即时更新（回车确认）
            Runnable apply = this::applyFields;
            widthField.setOnAction(e -> apply.run());
            heightField.setOnAction(e -> apply.run());

            Label previewTitle = new Label("贴图预览（按当前宽高缩放）");
            Pane previewPane = new Pane(previewCanvas);

            VBox panel = new VBox(10, infoLabel, fields, previewTitle, previewPane, statusLabel);
            panel.setPadding(new Insets(10));
            VBox.setVgrow(previewPane, Priority.ALWAYS);
            statusLabel.setWrapText(true);
            statusLabel.setStyle("-fx-text-fill: #444;");
            return panel;
        }

        private Button originalSizeButton() {
            Button btn = new Button("按原图尺寸");
            btn.setOnAction(e -> {
                if (selected == null) return;
                Image img = loadTexture(selected.className);
                if (img == null || img.isError() || img.getWidth() <= 0) {
                    statusLabel.setText("未找到贴图 assets/" + snake(selected.className) + ".png，无法读取原图尺寸");
                    return;
                }
                widthField.setText(num(img.getWidth()));
                heightField.setText(num(img.getHeight()));
                applyFields();
                statusLabel.setText("已按原图尺寸填充：" + num(img.getWidth()) + " × " + num(img.getHeight()));
            });
            return btn;
        }

        private HBox statusBar() {
            HBox bar = new HBox(statusLabel);
            bar.setPadding(new Insets(6, 10, 6, 10));
            bar.setStyle("-fx-background-color: #f0f0f0;");
            return bar;
        }

        // ================= 数据与交互 =================
        /** 行条目：类 + 分组（分组用于排序展示） */
        private final class ClassRow {
            final String className;
            final String group;

            ClassRow(String className, String group) {
                this.className = className;
                this.group = group;
            }

            @Override
            public String toString() {
                SizeTable.Entry entry = table.get(className);
                String size = entry != null
                        ? num(entry.getWidth()) + "×" + num(entry.getHeight())
                        : "跟随 default";
                return "[" + groupLabel(group) + "]  " + className + "   " + size;
            }
        }

        /** 重载 JSON + 重扫类清单 + 刷新显示 */
        private void refreshAll() {
            table.read();                       // 仓库文件优先，退 classpath
            List<ClassRow> rows = scanRows();
            classList.getItems().setAll(rows);
            statusLabel.setText("已载入 " + table.entryList().size() + " 条配置 / 枚举到 "
                    + rows.size() + " 个具体类（留空=跟随 default "
                    + num(table.getDefault().getWidth()) + "×" + num(table.getDefault().getHeight()) + "）");
            selected = null;
            infoLabel.setText("（未选中类）");
            widthField.clear();
            heightField.clear();
            clearPreview();
        }

        private void selectRow(ClassRow row) {
            if (row == null) return;
            applyFields();                      // 先落上一条的编辑
            selected = row;
            SizeTable.Entry entry = table.get(row.className);
            if (entry != null) {
                widthField.setText(num(entry.getWidth()));
                heightField.setText(num(entry.getHeight()));
                infoLabel.setText(row.className + "（" + groupLabel(row.group) + "）  显式配置");
            } else {
                widthField.clear();
                heightField.clear();
                infoLabel.setText(row.className + "（" + groupLabel(row.group) + "）  跟随 default "
                        + num(table.getDefault().getWidth()) + "×" + num(table.getDefault().getHeight()));
            }
            drawPreview(row.className);
        }

        /** 把宽/高输入框落到表（空或非法=移除条目，跟随 default） */
        private void applyFields() {
            if (selected == null) return;
            try {
                double w = widthField.getText().isBlank() ? -1 : Double.parseDouble(widthField.getText().trim());
                double h = heightField.getText().isBlank() ? -1 : Double.parseDouble(heightField.getText().trim());
                if ((w > 0) != (h > 0)) {
                    statusLabel.setText("宽/高需同时填写或同时留空（留空=跟随 default）");
                    return;
                }
                table.put(selected.className, w, h);
                refreshRowLabel();
                drawPreview(selected.className);
            } catch (NumberFormatException ex) {
                statusLabel.setText("宽/高必须是数字（允许小数）");
            }
        }

        /** 刷新左侧行显示（refresh 只重绘单元格，不触碰 items，避免触发选择监听器递归） */
        private void refreshRowLabel() {
            classList.refresh();
        }

        // ================= 反射扫描 =================
        /** 扫描 com.kingdom.game.model 下 GameObject 具体子类，按子包分组排序 */
        private List<ClassRow> scanRows() {
            List<ClassRow> rows = new ArrayList<>();
            try {
                ClassLoader cl = SizeEditorTool.class.getClassLoader();
                URL url = cl.getResource("com/kingdom/game/model");
                if (url == null) {
                    statusLabel.setText("未找到 model 包（请在仓库根目录运行）");
                    return rows;
                }
                Path dir = Paths.get(url.toURI());
                try (Stream<Path> paths = Files.walk(dir)) {
                    paths.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
                        String rel = dir.relativize(p).toString().replace(File.separatorChar, '.');
                        String name = rel.substring(0, rel.length() - ".class".length());
                        String fqcn = "com.kingdom.game.model." + name;
                        try {
                            Class<?> cls = Class.forName(fqcn);
                            if (!GameObject.class.isAssignableFrom(cls)
                                    || cls.isInterface()
                                    || Modifier.isAbstract(cls.getModifiers())
                                    || cls.getName().contains("$")) {
                                return;
                            }
                            String group = name.contains(".")
                                    ? name.substring(0, name.indexOf('.')) : "other";
                            rows.add(new ClassRow(cls.getSimpleName(), group));
                        } catch (Throwable ignored) {
                            // 扫描期个别类加载失败直接跳过
                        }
                    });
                }
            } catch (Exception e) {
                statusLabel.setText("类扫描失败: " + e.getMessage());
            }
            rows.sort((a, b) -> {
                int ga = groupIndex(a.group);
                int gb = groupIndex(b.group);
                if (ga != gb) return Integer.compare(ga, gb);
                return a.className.compareTo(b.className);
            });
            return rows;
        }

        private static int groupIndex(String g) {
            for (int i = 0; i < GROUP_ORDER.length; i++) {
                if (GROUP_ORDER[i].equals(g)) return i;
            }
            return GROUP_ORDER.length;
        }

        private static String groupLabel(String g) {
            int i = groupIndex(g);
            return i < GROUP_LABELS.length ? GROUP_LABELS[i] : g;
        }

        // ================= 贴图预览 =================
        /** 类名 camel → snake（EliteArrowTower → elite_arrow_tower） */
        private static String snake(String camel) {
            StringBuilder sb = new StringBuilder();
            for (char c : camel.toCharArray()) {
                if (Character.isUpperCase(c) && sb.length() > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            }
            return sb.toString();
        }

        /** 优先仓库 resources 目录（开发期最新），退回 classpath */
        private Image loadTexture(String className) {
            String rel = "assets/" + snake(className) + ".png";
            try {
                File repo = findRepoResourcesDir();
                if (repo != null) {
                    File f = new File(repo, rel);
                    if (f.isFile()) {
                        try (FileInputStream in = new FileInputStream(f)) {
                            return new Image(in);
                        }
                    }
                }
                try (var in = SizeEditorTool.class.getResourceAsStream("/" + rel)) {
                    if (in != null) return new Image(in);
                }
            } catch (IOException ignored) {
            }
            return null;
        }

        private void drawPreview(String className) {
            GraphicsContext gc = previewCanvas.getGraphicsContext2D();
            gc.setFill(javafx.scene.paint.Color.DIMGRAY);
            gc.fillRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
            Image img = loadTexture(className);
            if (img == null || img.isError() || img.getWidth() <= 0) {
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.fillText("缺贴图 assets/" + snake(className) + ".png", 12, 24);
                return;
            }
            SizeTable.Entry entry = table.entryByName(className);
            double w = entry.getWidth();
            double h = entry.getHeight();
            double cw = previewCanvas.getWidth();
            double ch = previewCanvas.getHeight();
            // 预览比例：1 画布像素 = 2 实体像素（大实体自动再缩），并画比例尺
            double scale = Math.min(2.0, Math.min((cw - 40) / w, (ch - 40) / h));
            double dw = w * scale;
            double dh = h * scale;
            gc.drawImage(img, (cw - dw) / 2, (ch - dh) / 2, dw, dh);
            gc.setFill(javafx.scene.paint.Color.YELLOW);
            gc.fillText("游戏内实际: " + num(w) + " × " + num(h) + "（预览比例 1:" + num(scale) + "）", 8, ch - 10);
        }

        private void clearPreview() {
            GraphicsContext gc = previewCanvas.getGraphicsContext2D();
            gc.setFill(javafx.scene.paint.Color.DIMGRAY);
            gc.fillRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
        }

        // ================= 保存 =================
        private void saveToRepo() {
            applyFields();
            if (table.saveToRepo()) {
                statusLabel.setText("已保存到 src/main/resources/assets/sizes.json（重启游戏生效）");
            } else {
                error("保存失败", "未找到 src/main/resources（请以仓库根目录为工作目录运行），可改用「另存为…」");
            }
        }

        private void saveAs() {
            applyFields();
            FileChooser fc = new FileChooser();
            fc.setTitle("另存为尺寸 JSON");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("尺寸 JSON", "*.json"));
            fc.setInitialFileName("sizes.json");
            File file = fc.showSaveDialog(null);
            if (file == null) return;
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getParentFile(), file.getName() + ".json");
            }
            try {
                java.nio.file.Files.writeString(file.toPath(), table.toJson(),
                        java.nio.charset.StandardCharsets.UTF_8);
                statusLabel.setText("已另存为 " + file.getPath());
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        private void error(String title, String msg) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        }

        private static File findRepoResourcesDir() {
            File dir = new File(System.getProperty("user.dir"));
            for (int i = 0; i < 8 && dir != null; i++) {
                File candidate = new File(dir, "src/main/resources");
                if (candidate.isDirectory()) return candidate;
                dir = dir.getParentFile();
            }
            return null;
        }

        private static String num(double v) {
            if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
            return String.valueOf(v);
        }
    }
}
