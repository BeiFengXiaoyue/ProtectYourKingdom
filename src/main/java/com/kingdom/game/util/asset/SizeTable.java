package com.kingdom.game.util.asset;

import com.kingdom.game.model.GameObject;
import com.kingdom.game.util.json.MiniJson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SizeTable —— 实体视觉尺寸表仓库（素材方 E 维护，JSON 驱动）。
 *
 * 尺寸数据源 = assets/sizes.json（契约见 docs/视觉尺寸配置规范.md）：
 * <pre>
 * {
 *   "default": { "width": 20, "height": 20 },
 *   "entities": { "NormalEnemy": {"width":20,"height":20}, ... }
 * }
 * </pre>
 * 键 = GameObject 具体子类的 getSimpleName()。
 *
 * 回退顺序（严格）：entities[类名] → default → 内置兜底 20×20；
 * 全程不抛异常、不阻断游戏启动，加载失败仅打印警告（对齐 Assets 容错风格）。
 *
 * 双轨读取（对齐 AnimTable/AnimLibrary 范式）：
 * - 运行期 {@link #entryFor}/{@link #preload()}：classpath 只读；
 * - 编辑器 {@link #read()}：优先仓库文件 src/main/resources/assets/sizes.json，
 *   {@link #saveToRepo()} 合并写回（保留未知条目、同 key 覆盖）。
 *
 * 无 JavaFX 依赖；懒加载静态单例，实体构造期 entryFor 一次并固化尺寸。
 */
public final class SizeTable {

    /** 仓库内 JSON 位置（编辑器读写用） */
    public static final String REPO_JSON_PATH = "src/main/resources/assets/sizes.json";
    /** classpath 位置（运行期读取用） */
    public static final String CLASSPATH_JSON = "/assets/sizes.json";
    /** 内置兜底尺寸 */
    private static final double FALLBACK_SIZE = 20;

    private static final SizeTable INSTANCE = new SizeTable();

    /** 单例：GameObject 构造期与编辑器共用同一张表 */
    public static SizeTable getInstance() {
        return INSTANCE;
    }

    /** 单条尺寸记录 */
    public static final class Entry {
        private final String className;
        private double width;
        private double height;

        Entry(String className, double width, double height) {
            this.className = className;
            this.width = width;
            this.height = height;
        }

        public String getClassName() { return className; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }

        void setWidth(double width) { this.width = width; }
        void setHeight(double height) { this.height = height; }
    }

    /** 尺寸表：键 = 类名；value = null 表示该类显式“跟随 default”（编辑器留空语义） */
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    /** 顶层 default 尺寸 */
    private Entry defaultEntry = new Entry("default", FALLBACK_SIZE, FALLBACK_SIZE);
    private boolean loaded = false;

    private SizeTable() {
    }

    // ================= 运行期 API =================
    /** 触发一次加载并缓存；缺文件/解析失败仅打印警告 */
    public void preload() {
        ensureLoaded();
    }

    /**
     * 按实体运行时类查尺寸条目：entities[类名] → default（内置兜底 20×20）。
     * 永远返回非 null。
     */
    public Entry entryFor(Class<? extends GameObject> cls) {
        ensureLoaded();
        if (cls != null) {
            Entry hit = entries.get(cls.getSimpleName());
            if (hit != null) return hit;
        }
        return defaultEntry;
    }

    /** 按类名查尺寸条目（编辑器用）：entities[类名] → default，永远返回非 null */
    public Entry entryByName(String className) {
        ensureLoaded();
        if (className != null) {
            Entry hit = entries.get(className);
            if (hit != null) return hit;
        }
        return defaultEntry;
    }

    // ================= 编辑器 API =================
    /**
     * 编辑器读表：优先仓库文件 src/main/resources/assets/sizes.json，
     * 否则退回 classpath，否则返回“仅 default”的空表。每次调用重新读盘。
     */
    public SizeTable read() {
        Map<String, Object> root = readRepoJson();
        if (root == null) root = readClasspathJson();
        applyJson(root);
        return this;
    }

    /**
     * 合并写回仓库文件：保留文件里已有但本次未枚举到的条目，同 key 覆盖。
     *
     * @return 写入成功与否
     */
    public boolean saveToRepo() {
        File repo = findRepoResourcesDir();
        if (repo == null) {
            System.err.println("[SizeTable] 未找到 src/main/resources，保存失败");
            return false;
        }
        File file = new File(new File(repo, "assets"), "sizes.json");
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                System.err.println("[SizeTable] 无法创建目录 " + file.getParentFile());
                return false;
            }
            Files.writeString(file.toPath(), toJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("[SizeTable] 保存失败: " + e.getMessage());
            return false;
        }
    }

    /** 条目列表（编辑器展示；不含 default） */
    public List<Entry> entryList() {
        ensureLoaded();
        return new ArrayList<>(entries.values());
    }

    /** 取某类条目；不存在返回 null（编辑器“跟随 default”语义） */
    public Entry get(String className) {
        ensureLoaded();
        return entries.get(className);
    }

    /** 放入/覆盖条目（width/height 非正数视为“跟随 default”，移除显式条目） */
    public void put(String className, double width, double height) {
        ensureLoaded();
        if (width > 0 && height > 0) {
            Entry existing = entries.get(className);
            if (existing != null) {
                existing.setWidth(width);
                existing.setHeight(height);
            } else {
                entries.put(className, new Entry(className, width, height));
            }
        } else {
            entries.remove(className);
        }
    }

    /** 移除条目（回退 default） */
    public void remove(String className) {
        ensureLoaded();
        entries.remove(className);
    }

    /** 类名清单（排序，编辑器枚举展示用） */
    public List<String> classNameList() {
        ensureLoaded();
        List<String> names = new ArrayList<>(entries.keySet());
        names.sort(String::compareTo);
        return names;
    }

    /** 当前 default 尺寸（编辑器展示“跟随 default”的生效值） */
    public Entry getDefault() {
        ensureLoaded();
        return defaultEntry;
    }

    // ================= JSON 读写 =================
    private synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        applyJson(readClasspathJson());
    }

    /** 读 classpath JSON；缺失/失败返回 null */
    private Map<String, Object> readClasspathJson() {
        try (InputStream in = SizeTable.class.getResourceAsStream(CLASSPATH_JSON)) {
            if (in == null) {
                System.err.println("[SizeTable] 未找到 " + CLASSPATH_JSON + "，全部实体回退默认尺寸");
                return null;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) new MiniJson(json).parse();
            return root;
        } catch (IOException | RuntimeException e) {
            System.err.println("[SizeTable] 尺寸表解析失败: " + e.getMessage());
            return null;
        }
    }

    /** 读仓库 JSON（编辑器用）；缺失/失败返回 null */
    private Map<String, Object> readRepoJson() {
        File repo = findRepoResourcesDir();
        if (repo == null) return null;
        File file = new File(new File(repo, "assets"), "sizes.json");
        if (!file.isFile()) return null;
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) new MiniJson(json).parse();
            return root;
        } catch (IOException | RuntimeException e) {
            System.err.println("[SizeTable] 仓库尺寸表解析失败: " + e.getMessage());
            return null;
        }
    }

    /** 把 JSON 根对象套用到当前表（整体重建 entries/default） */
    private void applyJson(Map<String, Object> root) {
        entries.clear();
        defaultEntry = new Entry("default", FALLBACK_SIZE, FALLBACK_SIZE);
        if (root == null) return;

        Object def = root.get("default");
        if (def instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dm = (Map<String, Object>) def;
            double w = asPositive(dm.get("width"));
            double h = asPositive(dm.get("height"));
            if (w > 0 && h > 0) defaultEntry = new Entry("default", w, h);
        }

        Object ents = root.get("entities");
        if (ents instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) ents;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (!(e.getValue() instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> sm = (Map<String, Object>) e.getValue();
                double w = asPositive(sm.get("width"));
                double h = asPositive(sm.get("height"));
                if (w > 0 && h > 0) entries.put(e.getKey(), new Entry(e.getKey(), w, h));
                // 任一缺失/非法 → 视同未配置，走 default 回退
            }
        }
    }

    private static double asPositive(Object v) {
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d > 0) return d;
        }
        return -1;
    }

    /** 序列化为契约格式（default + entities；entities 按类名排序，稳定输出） */
    public String toJson() {
        ensureLoaded();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"default\": {\"width\":").append(num(defaultEntry.getWidth()))
          .append(",\"height\":").append(num(defaultEntry.getHeight())).append("},\n");
        List<String> names = new ArrayList<>(entries.keySet());
        names.sort(String::compareTo);
        sb.append("  \"entities\": {");
        boolean first = true;
        for (String name : names) {
            Entry e = entries.get(name);
            if (!first) sb.append(",");
            first = false;
            sb.append("\n    \"").append(name).append("\": {\"width\":").append(num(e.getWidth()))
              .append(",\"height\":").append(num(e.getHeight())).append("}");
        }
        if (!names.isEmpty()) sb.append("\n  ");
        sb.append("}\n}\n");
        return sb.toString();
    }

    private static String num(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    /** 从当前工作目录向上查找 src/main/resources（对齐 AnimLibrary 同名规则） */
    private static File findRepoResourcesDir() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, "src/main/resources");
            if (candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
        }
        return null;
    }
}
