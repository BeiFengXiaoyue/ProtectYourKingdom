package com.kingdom.game.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AnimTable —— 敌人行为动画表（每个敌人一个 JSON）。
 *
 * JSON 结构（路径为相对 assets/ 的图片地址）：
 * <pre>
 * {
 *   "name": "normal_enemy",
 *   "interval": 6,
 *   "modes": {
 *     "idle":   ["enemies/normal_enemy/idle_0.png", ...],
 *     "walk":   [...],
 *     "attack": [...]
 *   }
 * }
 * </pre>
 * - interval：每张关键帧停留的游戏 tick 数（默认 6，约 0.1s/帧）；
 * - modes 的键即“行为模式”，**可随意增删**，加载器自动适配。
 * JSON 读写复用同包 MapRoute.MiniJson（极简解析器）。
 */
public final class AnimTable {

    private String name;
    private int interval;
    private final Map<String, List<String>> modes = new LinkedHashMap<>();

    public AnimTable(String name, int interval) {
        this.name = name == null ? "" : name;
        this.interval = Math.max(1, interval);
    }

    /** 新建空表（默认三行为模式：idle / walk / attack） */
    public static AnimTable template(String name) {
        AnimTable t = new AnimTable(name, 6);
        t.addMode("idle");
        t.addMode("walk");
        t.addMode("attack");
        return t;
    }

    // ================= 访问器 =================
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }
    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = Math.max(1, interval); }

    /** 行为模式名（保持添加顺序） */
    public List<String> getModeNames() {
        return new ArrayList<>(modes.keySet());
    }

    /** 某模式的帧列表（内部引用，编辑器可直接改顺序/删除） */
    public List<String> getFrames(String mode) {
        return modes.computeIfAbsent(mode, k -> new ArrayList<>());
    }

    public boolean hasMode(String mode) {
        return mode != null && modes.containsKey(mode);
    }

    // ================= 增删改 =================
    /** 新增行为模式（已存在返回 false） */
    public boolean addMode(String mode) {
        if (mode == null || mode.isBlank() || modes.containsKey(mode)) return false;
        modes.put(mode, new ArrayList<>());
        return true;
    }

    /** 删除行为模式（连同其帧） */
    public boolean removeMode(String mode) {
        return mode != null && modes.remove(mode) != null;
    }

    /** 追加一帧；模式不存在会自动创建 */
    public void addFrame(String mode, String relativePath) {
        getFrames(mode).add(relativePath);
    }

    public boolean removeFrameAt(String mode, int index) {
        List<String> frames = modes.get(mode);
        if (frames == null || index < 0 || index >= frames.size()) return false;
        frames.remove(index);
        return true;
    }

    /** 帧内移动：from 移到 to（0 基，越界自动夹紧） */
    public boolean moveFrame(String mode, int from, int to) {
        List<String> frames = modes.get(mode);
        if (frames == null || from < 0 || from >= frames.size()) return false;
        to = Math.max(0, Math.min(frames.size() - 1, to));
        String f = frames.remove(from);
        frames.add(to, f);
        return true;
    }

    // ================= JSON =================
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escape(name)).append("\",\n");
        sb.append("  \"interval\": ").append(interval).append(",\n");
        sb.append("  \"modes\": {");
        List<String> names = getModeNames();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n    \"").append(escape(names.get(i))).append("\": [");
            List<String> frames = modes.get(names.get(i));
            for (int j = 0; j < frames.size(); j++) {
                if (j > 0) sb.append(",");
                sb.append("\"").append(escape(frames.get(j))).append("\"");
            }
            sb.append("]");
        }
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static AnimTable fromJson(String json) {
        Object root = new MapRoute.MiniJson(json).parse();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("动画 JSON 顶层必须是对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;

        String name = obj.containsKey("name") && obj.get("name") != null
                ? String.valueOf(obj.get("name")) : "";
        int interval = obj.get("interval") instanceof Number
                ? ((Number) obj.get("interval")).intValue() : 6;

        AnimTable t = new AnimTable(name, interval);
        Object modesObj = obj.get("modes");
        if (modesObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) modesObj;
            for (Map.Entry<String, Object> e : map.entrySet()) {
                t.addMode(e.getKey());
                if (e.getValue() instanceof List) {
                    for (Object f : (List<?>) e.getValue()) {
                        if (f != null) t.getFrames(e.getKey()).add(String.valueOf(f));
                    }
                }
            }
        }
        return t;
    }

    // ================= classpath 加载（带缓存）=================
    private static final Map<String, AnimTable> CACHE = new LinkedHashMap<>();

    /**
     * 从 classpath 读取动画表。
     *
     * @param resourcePath 如 "/assets/animations/normal_enemy.json"
     */
    public static AnimTable loadFromClasspath(String resourcePath) {
        AnimTable cached = CACHE.get(resourcePath);
        if (cached != null) return cached;
        try (java.io.InputStream in = AnimTable.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            AnimTable t = fromJson(json);
            CACHE.put(resourcePath, t);
            return t;
        } catch (Exception e) {
            System.err.println("[AnimTable] 加载动画表失败: " + resourcePath + " -> " + e.getMessage());
            return null;
        }
    }
}
