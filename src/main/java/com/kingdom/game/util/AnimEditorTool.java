package com.kingdom.game.util;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnimEditorTool —— 单位行为动画 JSON 编辑器（敌人/友方/防御塔，按类别子目录管理）。
 * 入口不继承 Application，窗口在嵌套类 {@link AnimEditorApp}（避免 JavaFX 模块检查报错）。
 *
 * 功能（经 AnimLibrary 按“类别 + 单位名”管理，同类别同名去重）：
 * - 顶栏选类别：敌人(enemies) / 友方(allies) / 防御塔(towers)；
 * - 单位下拉：该类别已有表 + 已知单位类名（新建模板）+ 手动输入；
 * - 行为模式可增删；每个模式可逐张“选图添加关键帧”（拷入 assets/&lt;类别&gt;/&lt;名&gt;/），支持上移/下移/删除；
 * - interval、轮播预览；保存按类别落 assets/animations/&lt;类别&gt;/&lt;名&gt;.json；可删除该单位。
 */
public final class AnimEditorTool {

    private AnimEditorTool() {
    }

    public static void main(String[] args) {
        Application.launch(AnimEditorApp.class, args);
    }

    /** 真正的 JavaFX 应用（public static，供 launch 反射） */
    public static final class AnimEditorApp extends Application {

        /** 各类别已知的 Java 类名（类名 → 表名由 camelToSnake 转换） */
        private static final Map<String, List<String>> KNOWN_CLASSES = new LinkedHashMap<>();
        static {
            KNOWN_CLASSES.put(AnimTable.KIND_ENEMIES,
                    List.of("NormalEnemy", "FastEnemy", "TankEnemy", "BossEnemy"));
            KNOWN_CLASSES.put(AnimTable.KIND_ALLIES, List.of("Soldier"));
            KNOWN_CLASSES.put(AnimTable.KIND_TOWERS, List.of("ArrowTower"));
        }
        private static final String CUSTOM_ITEM = "…手动输入其他名字";

        private AnimTable table = AnimTable.template(AnimTable.KIND_ENEMIES, "");
        private String currentKind = AnimTable.KIND_ENEMIES;
        private boolean uiBusy = false;   // adopt() 程序化切类别时防回调重入

        private final ChoiceBox<String> kindChoice = new ChoiceBox<>();
        private final ChoiceBox<String> unitChoice = new ChoiceBox<>();
        private final Map<String, String> choiceToUnit = new LinkedHashMap<>();
        private final TextField unitNameField = new TextField();
        private final TextField intervalField = new TextField("6");
        private final ListView<String> modeList = new ListView<>();
        private final ListView<String> frameList = new ListView<>();
        private final ImageView previewView = new ImageView();
        private final Label statusLabel = new Label("选择类别与单位后即可编辑动画。");
        private final Label previewLabel = new Label("（无预览素材）");

        // 轮播预览
        private AnimationTimer playTimer;
        private final List<Image> playImages = new ArrayList<>();
        private int playIndex = 0;
        private long playTick = 0;
        private boolean playing = false;

        @Override
        public void start(Stage stage) {
            BorderPane root = new BorderPane();
            root.setTop(buildTopBar());
            BorderPane center = new BorderPane();
            center.setLeft(buildModePanel());
            center.setCenter(buildPreviewArea());
            center.setRight(buildFramePanel());
            root.setCenter(center);
            root.setBottom(statusLabel);

            Scene scene = new Scene(root, 1080, 620);
            stage.setTitle("王国保卫战 · 单位动画编辑器（敌人/友方/防御塔）");
            stage.setScene(scene);
            stage.show();

            kindChoice.setOnAction(e -> onKindChanged());
            unitChoice.setOnAction(e -> onUnitChoice());
            unitNameField.setOnAction(e -> loadCurrentUnit());
            modeList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> onModeSelected());
            frameList.getSelectionModel().selectedItemProperty()
                    .addListener((obs, o, n) -> refreshPreview());

            populateKindChoices();
            // 默认“敌人”类别并自动载入该类别第一个已有单位
            kindChoice.getSelectionModel().select(displayOfKind(AnimTable.KIND_ENEMIES));
            populateUnitChoices();
            refreshAll();
            autoLoadFirstExisting();
        }

        // ================= 类别/单位选择 =================
        private String displayOfKind(String kind) {
            switch (kind) {
                case AnimTable.KIND_ENEMIES: return "敌人 (enemies)";
                case AnimTable.KIND_ALLIES:  return "友方 (allies)";
                case AnimTable.KIND_TOWERS:  return "防御塔 (towers)";
                default: return "敌人 (enemies)";
            }
        }

        private String kindOfDisplay(String display) {
            for (String kind : new String[]{
                    AnimTable.KIND_ENEMIES, AnimTable.KIND_ALLIES, AnimTable.KIND_TOWERS}) {
                if (displayOfKind(kind).equals(display)) return kind;
            }
            return AnimTable.KIND_ENEMIES;
        }

        private void populateKindChoices() {
            kindChoice.getItems().setAll(
                    displayOfKind(AnimTable.KIND_ENEMIES),
                    displayOfKind(AnimTable.KIND_ALLIES),
                    displayOfKind(AnimTable.KIND_TOWERS));
        }

        /** 刷新单位下拉：当前类别已有表 + 已知单位类 + 手动输入 */
        private void populateUnitChoices() {
            choiceToUnit.clear();
            unitChoice.getItems().clear();
            List<String> existing = AnimLibrary.list(currentKind);

            for (String cls : KNOWN_CLASSES.getOrDefault(currentKind, List.of())) {
                String key = camelToSnake(cls);
                boolean has = existing.contains(key);
                String disp = cls + (has ? "（已有）" : "（新建模板）");
                choiceToUnit.put(disp, key);
                unitChoice.getItems().add(disp);
            }
            for (String n : existing) {
                boolean covered = false;
                for (String cls : KNOWN_CLASSES.getOrDefault(currentKind, List.of())) {
                    if (camelToSnake(cls).equals(n)) { covered = true; break; }
                }
                if (!covered) {
                    String disp = n + "（已有·自定义）";
                    choiceToUnit.put(disp, n);
                    unitChoice.getItems().add(disp);
                }
            }
            unitChoice.getItems().add(CUSTOM_ITEM);
        }

        private void onKindChanged() {
            if (uiBusy) return;
            stopPlay();
            String kind = kindOfDisplay(kindChoice.getValue());
            currentKind = kind;
            populateUnitChoices();
            table = AnimTable.template(currentKind, "");
            unitNameField.setText("");
            status("类别已切换为 " + displayOfKind(kind) + "，请选择或新建单位");
            refreshAll();
        }

        private void onUnitChoice() {
            String disp = unitChoice.getValue();
            if (disp == null) return;
            String key = choiceToUnit.get(disp);
            if (key == null) {                 // 手动输入
                unitNameField.requestFocus();
                return;
            }
            loadUnit(currentKind, key);
        }

        private void loadCurrentUnit() {
            loadUnit(currentKind, currentName());
        }

        private void loadUnit(String kind, String name) {
            if (name == null || name.isBlank()) {
                status("单位名不能为空");
                return;
            }
            AnimTable t = AnimLibrary.read(kind, name);
            if (t == null) {
                t = AnimTable.template(kind, name);
                status("未找到「" + kind + "/" + name + "」，已新建模板");
            } else {
                status("已载入「" + kind + "/" + name + "」（同名覆盖修改）");
            }
            adopt(t);
        }

        private void autoLoadFirstExisting() {
            List<String> existing = AnimLibrary.list(currentKind);
            if (existing.isEmpty()) {
                status("当前类别暂无动画表，请从下拉新建或手动输入单位名");
                return;
            }
            String first = existing.get(0);
            unitNameField.setText(first);
            loadUnit(currentKind, first);
        }

        private void adopt(AnimTable t) {
            table = t == null ? AnimTable.template(currentKind, "") : t;
            currentKind = table.getKind();
            uiBusy = true;
            try {
                kindChoice.getSelectionModel().select(displayOfKind(currentKind));
            } finally {
                uiBusy = false;
            }
            unitNameField.setText(table.getName());
            intervalField.setText(String.valueOf(table.getInterval()));
            stopPlay();
            populateUnitChoices();
            refreshAll();
        }

        // ================= UI 构建 =================
        private HBox buildTopBar() {
            Button load = new Button("载入");
            load.setOnAction(e -> loadCurrentUnit());
            Button openJson = new Button("打开 JSON…");
            openJson.setOnAction(e -> openJsonFile());
            Button save = new Button("保存");
            save.setOnAction(e -> saveToGame());
            Button saveAs = new Button("另存为…");
            saveAs.setOnAction(e -> saveAsJson());
            Button del = new Button("删除该单位");
            del.setOnAction(e -> deleteUnit());

            unitNameField.setPrefWidth(140);
            unitChoice.setPrefWidth(220);
            kindChoice.setPrefWidth(140);
            intervalField.setPrefWidth(50);

            HBox bar = new HBox(8, new Label("类别:"), kindChoice,
                    new Label("单位:"), unitChoice,
                    new Label("名字:"), unitNameField,
                    new Label("interval:"), intervalField,
                    load, openJson, save, saveAs, del);
            bar.setPadding(new Insets(8));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color: #e0e0e0;");
            return bar;
        }

        private VBox buildModePanel() {
            modeList.setPrefWidth(140);
            modeList.setPrefHeight(400);
            Button add = new Button("+ 添加模式");
            add.setOnAction(e -> addMode());
            Button del = new Button("- 删除模式");
            del.setOnAction(e -> removeMode());
            VBox box = new VBox(8, new Label("行为模式"), modeList, add, del);
            box.setPadding(new Insets(10));
            return box;
        }

        private VBox buildPreviewArea() {
            previewView.setFitWidth(140);
            previewView.setFitHeight(140);
            previewView.setPreserveRatio(true);
            previewView.setStyle("-fx-background-color: #fafafa; -fx-border-color: #ccc;");

            Button play = new Button("轮播预览");
            play.setOnAction(e -> togglePlay());

            previewLabel.setWrapText(true);
            previewLabel.setFont(Font.font(12));

            VBox box = new VBox(10, previewLabel, previewView, play);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(12));
            VBox.setVgrow(box, Priority.ALWAYS);
            return box;
        }

        private VBox buildFramePanel() {
            frameList.setPrefWidth(280);
            frameList.setPrefHeight(360);

            Button add = new Button("+ 添加关键帧(选图)");
            add.setOnAction(e -> addFrame());
            Button up = new Button("上移");
            up.setOnAction(e -> moveFrame(-1));
            Button down = new Button("下移");
            down.setOnAction(e -> moveFrame(1));
            Button del = new Button("- 删除该帧");
            del.setOnAction(e -> removeFrame());

            VBox box = new VBox(8, new Label("当前模式的关键帧"), frameList,
                    new HBox(6, add), new HBox(6, up, down, del));
            box.setPadding(new Insets(10));
            return box;
        }

        // ================= 数据操作 =================
        private String currentName() {
            String n = unitNameField.getText() == null ? "" : unitNameField.getText().trim();
            return n.replaceAll("[^A-Za-z0-9_\\-]", "_");
        }

        private String currentMode() {
            return modeList.getSelectionModel().getSelectedItem();
        }

        private void openJsonFile() {
            FileChooser fc = jsonChooser("打开动画 JSON", false);
            File f = fc.showOpenDialog(null);
            if (f == null) return;
            try {
                AnimTable t = AnimTable.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                adopt(t);
                status("已从文件载入：" + f.getPath());
            } catch (IOException | RuntimeException ex) {
                error("载入失败", ex.getMessage());
            }
        }

        private void saveToGame() {
            if (!applyNameAndInterval()) return;
            File f = AnimLibrary.save(table);
            if (f == null) {
                status("未找到 src/main/resources（请以仓库根目录运行），已改为另存为");
                saveAsJson();
                return;
            }
            populateUnitChoices();
            status("已保存到 " + f.getPath() + "（同类别同名去重）");
        }

        private void saveAsJson() {
            applyNameAndInterval();
            FileChooser fc = jsonChooser("另存为动画 JSON", true);
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            if (!f.getName().toLowerCase().endsWith(".json")) {
                f = new File(f.getParentFile(), f.getName() + ".json");
            }
            try {
                Files.writeString(f.toPath(), table.toJson(), StandardCharsets.UTF_8);
                status("已另存为 " + f.getPath());
            } catch (IOException ex) {
                error("保存失败", ex.getMessage());
            }
        }

        private boolean applyNameAndInterval() {
            String name = currentName();
            if (name.isEmpty()) {
                status("单位名不能为空");
                return false;
            }
            int interval = 6;
            try {
                interval = Integer.parseInt(intervalField.getText().trim());
            } catch (NumberFormatException ignored) {
                status("interval 需为整数，已用默认 6");
            }
            table.setKind(currentKind);
            table.setName(name);
            table.setInterval(interval);
            unitNameField.setText(name);
            intervalField.setText(String.valueOf(interval));
            return true;
        }

        private void deleteUnit() {
            String kind = currentKind;
            String name = currentName();
            if (name.isEmpty()) return;
            boolean ok = AnimLibrary.delete(kind, name);
            status(ok ? "已删除「" + kind + "/" + name + "」的动画表"
                    : "未找到可删除的动画表「" + kind + "/" + name + "」");
            table = AnimTable.template(kind, "");
            unitNameField.setText("");
            populateUnitChoices();
            refreshAll();
        }

        // ================= 模式 =================
        private void addMode() {
            TextInputDialog dlg = new TextInputDialog("fire");
            dlg.setTitle("添加行为模式");
            dlg.setHeaderText("新模式名");
            dlg.setContentText("模式名（字母数字下划线）：");
            dlg.showAndWait().ifPresent(m -> {
                String mode = m.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
                if (mode.isEmpty() || !table.addMode(mode)) {
                    status("添加失败：空名或已存在「" + mode + "」");
                } else {
                    status("已添加模式「" + mode + "」");
                }
                refreshModeList(mode);
            });
        }

        private void removeMode() {
            String mode = currentMode();
            if (mode == null) return;
            if (!table.getFrames(mode).isEmpty()) {
                status("模式「" + mode + "」还有帧，请先清空再删除");
                return;
            }
            table.removeMode(mode);
            status("已删除模式「" + mode + "」");
            refreshModeList(null);
        }

        // ================= 帧 =================
        private void addFrame() {
            String name = currentName();
            String mode = currentMode();
            if (name.isEmpty() || mode == null) {
                status("请先输入单位名并选中一个行为模式");
                return;
            }
            File dir = AnimLibrary.framesDir(currentKind, name);
            if (dir == null) {
                status("未找到 src/main/resources（请以仓库根目录运行）");
                return;
            }
            FileChooser fc = new FileChooser();
            fc.setTitle("选择关键帧图片（第 " + (table.getFrames(mode).size() + 1) + " 张）");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("图片", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            File src = fc.showOpenDialog(null);
            if (src == null) return;

            if (!dir.exists() && !dir.mkdirs()) {
                error("拷贝失败", "无法创建目录 " + dir);
                return;
            }
            int idx = nextFrameIndex(dir, mode);
            String ext = extensionOf(src.getName());
            String fileName = mode + "_" + idx + ext;
            File dst = new File(dir, fileName);
            try {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                error("拷贝失败", ex.getMessage());
                return;
            }
            String rel = currentKind + "/" + name + "/" + fileName;
            table.addFrame(mode, rel);
            status("已添加帧：" + rel);
            refreshFrameList();
        }

        private void removeFrame() {
            String mode = currentMode();
            int idx = frameList.getSelectionModel().getSelectedIndex();
            if (mode == null || !table.removeFrameAt(mode, idx)) return;
            status("已删除帧 #" + idx);
            refreshFrameList();
        }

        private void moveFrame(int delta) {
            String mode = currentMode();
            int from = frameList.getSelectionModel().getSelectedIndex();
            if (mode == null || from < 0) return;
            table.moveFrame(mode, from, from + delta);
            refreshFrameList();
            frameList.getSelectionModel().select(from + delta);
        }

        private int nextFrameIndex(File dir, String mode) {
            int max = -1;
            File[] files = dir.listFiles((d, n) -> n.startsWith(mode + "_"));
            if (files != null) {
                for (File f : files) {
                    String n = f.getName();
                    try {
                        int idx = Integer.parseInt(n.substring(mode.length() + 1,
                                n.lastIndexOf('.')));
                        max = Math.max(max, idx);
                    } catch (Exception ignored) {
                        // 非数字序号忽略
                    }
                }
            }
            return max + 1;
        }

        private String extensionOf(String fileName) {
            int dot = fileName.lastIndexOf('.');
            String ext = dot >= 0 ? fileName.substring(dot).toLowerCase() : ".png";
            return ext.equals(".jpg") ? ".jpg" : ext;
        }

        // ================= 预览 =================
        private void onModeSelected() {
            refreshFrameList();
        }

        private void refreshAll() {
            refreshModeList(null);
            refreshPreview();
        }

        private void refreshModeList(String select) {
            modeList.getItems().setAll(table.getModeNames());
            if (select != null && table.hasMode(select)) {
                modeList.getSelectionModel().select(select);
            } else if (!modeList.getItems().isEmpty()) {
                modeList.getSelectionModel().select(0);
            }
            refreshFrameList();
        }

        private void refreshFrameList() {
            String mode = currentMode();
            if (mode == null) {
                frameList.getItems().clear();
            } else {
                frameList.getItems().setAll(table.getFrames(mode));
            }
            refreshPreview();
        }

        private void refreshPreview() {
            stopPlay();
            String mode = currentMode();
            if (mode == null) {
                previewView.setImage(null);
                previewLabel.setText("（先选一个行为模式）");
                return;
            }
            List<String> rels = table.getFrames(mode);
            if (rels.isEmpty()) {
                previewView.setImage(null);
                previewLabel.setText("模式「" + mode + "」暂无帧");
                return;
            }
            Image img = loadImage(rels.get(0));
            previewView.setImage(img);
            previewLabel.setText(img == null
                    ? "缺少素材：" + rels.get(0)
                    : "模式「" + mode + "」共 " + rels.size() + " 帧");
        }

        private void togglePlay() {
            if (playing) {
                stopPlay();
                return;
            }
            String mode = currentMode();
            if (mode == null) return;
            playImages.clear();
            for (String rel : table.getFrames(mode)) {
                Image img = loadImage(rel);
                if (img != null) playImages.add(img);
            }
            if (playImages.isEmpty()) {
                status("当前模式没有可加载的素材，无法预览");
                return;
            }
            playing = true;
            playIndex = 0;
            playTick = 0;
            previewView.setImage(playImages.get(0));
            int interval = table.getInterval();
            playTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    playTick++;
                    if (playTick >= interval && playImages.size() > 1) {
                        playTick = 0;
                        playIndex = (playIndex + 1) % playImages.size();
                        previewView.setImage(playImages.get(playIndex));
                    }
                }
            };
            playTimer.start();
            status("轮播预览中（interval=" + interval + "）");
        }

        private void stopPlay() {
            playing = false;
            if (playTimer != null) {
                playTimer.stop();
                playTimer = null;
            }
        }

        private Image loadImage(String relative) {
            File f = AnimLibrary.resolveFrameFile(relative);
            return f == null ? null : new Image(f.toURI().toString());
        }

        // ================= 工具 =================
        private FileChooser jsonChooser(String title, boolean save) {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("动画 JSON", "*.json"));
            return fc;
        }

        /** NormalEnemy → normal_enemy；ArrowTower → arrow_tower */
        private static String camelToSnake(String s) {
            if (s == null || s.isEmpty()) return s;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > 0) sb.append('_');
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private void status(String msg) {
            statusLabel.setText(msg);
        }

        private void error(String title, String msg) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(null);
            a.setContentText(msg);
            a.showAndWait();
        }
    }
}
