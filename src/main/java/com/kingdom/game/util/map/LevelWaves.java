package com.kingdom.game.util.map;

import com.kingdom.game.util.json.MiniJson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LevelWaves —— 关卡波次数据 + JSON 读写 + classpath 加载器（工具类）。
 *
 * 与 {@link MapRoute}/{@link TowerSpots} 同一套模式：波次由 util.editor.WaveEditorTool
 * 编辑并写入 maps/&lt;key&gt;/waves.json（经 util.map.MapLibrary），游戏启动时经
 * {@link #loadFromClasspath(String)}（MapLibrary.readWavesFromClasspath）读取。
 *
 * 数据契约见 docs/关卡波次配置规范.md：
 * - waves 数组顺序即游戏内波次顺序，**事实来源是数组下标**（id 仅阅读用，不一致仅告警）；
 * - maxPrepMs = −1 表示"未指定"，由接线方回退 GameConfig 全局默认（第 1 波本就忽略该字段）；
 * - 本类与工具只维护敌人 id 词汇表（无任何数值，数值归 GameConfig 按 id 补齐，规范 §4）。
 *
 * 加载容错（规范 §3.5，运行期不崩）：
 * - 单条 wave 结构非法（缺 groups、count&lt;1、intervalMs≤0）→ 跳过该波并告警；
 * - 组 type 不在词汇表 → 跳过该组并告警；
 * - 文件缺失/整体解析失败 → loadFromClasspath 返回 null，由调用方回退 GameConfig 全局行为。
 *
 * JSON schema（复用 util.json.MiniJson 解析）：
 * <pre>
 * {
 *   "name": "default_waves",
 *   "waves": [
 *     { "id": 1, "maxPrepMs": 5000,
 *       "groups": [ {"type":"normal_enemy","count":3,"intervalMs":1000} ] },
 *     ...
 *   ]
 * }
 * </pre>
 */
public final class LevelWaves {

    // ================= 敌人 id 词汇表（规范 §4）=================
    /**
     * 敌人 id → 工具显示名：编辑器下拉与校验的唯一来源，新敌人 = 在此加一行
     * （"三处同步：类 + AssetKey + 词汇表"之一，见规范 §4；本表不含任何数值）。
     */
    public static final Map<String, String> VOCABULARY = new LinkedHashMap<>();

    static {
        VOCABULARY.put("normal_enemy", "普通敌人");
        VOCABULARY.put("fast_enemy", "快速敌人");
        VOCABULARY.put("tank_enemy", "重甲敌人");
        VOCABULARY.put("boss_enemy", "首领敌人");
    }

    /** 词汇表显示名（未知 id 原样返回，供容错展示） */
    public static String labelOf(String type) {
        return VOCABULARY.getOrDefault(type, type);
    }

    /** loadFromClasspath 结果缓存（规范 §5.1 要求"带缓存"；运行期只读，key = resource 路径） */
    private static final Map<String, LevelWaves> CACHE = new ConcurrentHashMap<>();

    private String name;
    private final List<Wave> waves = new ArrayList<>();

    /** @param name 关卡名（仅标识用；缺省时保存侧用 "&lt;key&gt;_waves"） */
    public LevelWaves(String name) {
        this.name = name == null ? "" : name;
    }

    // ================= Getter / Setter =================
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "" : name; }

    /** 波次列表（有序；直接暴露内部列表供编辑器原地增删，运行期只读使用） */
    public List<Wave> getWaves() { return waves; }

    /** 一波：id 仅阅读用（事实来源是数组下标）；maxPrepMs = −1 表示未指定（回退全局默认） */
    public static final class Wave {
        private int id;
        private long maxPrepMs = -1;
        private final List<SpawnGroup> groups = new ArrayList<>();

        public Wave() { }

        public Wave(int id) {
            this.id = id;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        /** −1 = 未指定（接线方回退 GameConfig.waveIntermissionMs；第 1 波本就忽略） */
        public long getMaxPrepMs() { return maxPrepMs; }
        public void setMaxPrepMs(long maxPrepMs) { this.maxPrepMs = maxPrepMs; }

        /** 出怪组列表（有序；直接暴露内部列表供编辑器原地增删） */
        public List<SpawnGroup> getGroups() { return groups; }

        private String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("      \"id\": ").append(id);
            if (maxPrepMs >= 0) {
                sb.append(",\n      \"maxPrepMs\": ").append(maxPrepMs);
            }
            sb.append(",\n      \"groups\": [");
            for (int i = 0; i < groups.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\n        ").append(groups.get(i).toJson());
            }
            sb.append("\n      ]\n    }");
            return sb.toString();
        }
    }

    /** 出怪组：波内最小出怪单元（type ∈ 词汇表；HP/速度等数值一律不在此，归 GameConfig） */
    public static final class SpawnGroup {
        private String type = "normal_enemy";
        private int count = 1;
        private long intervalMs = 1000;

        public SpawnGroup() { }

        public SpawnGroup(String type, int count, long intervalMs) {
            this.type = type == null ? "" : type;
            this.count = count;
            this.intervalMs = intervalMs;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type == null ? "" : type; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public long getIntervalMs() { return intervalMs; }
        public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }

        private String toJson() {
            return "{\"type\":\"" + escape(type) + "\",\"count\":" + count
                    + ",\"intervalMs\":" + intervalMs + "}";
        }
    }

    // ================= 模板 =================
    /** 新建空表模板（编辑器"新建"用）：1 波 + 1 组 普通敌人×1@1000ms，maxPrepMs 未指定 */
    public static LevelWaves template(String name) {
        LevelWaves lw = new LevelWaves(name);
        Wave w = new Wave(1);
        w.getGroups().add(new SpawnGroup());
        lw.waves.add(w);
        return lw;
    }

    // ================= JSON 输出 =================
    /** 手写 StringBuilder（MiniJson 无序列化 API，同 TowerSpots 模式）；字段序稳定便于 diff */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escape(name)).append("\",\n");
        sb.append("  \"waves\": [");
        for (int i = 0; i < waves.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\n    ").append(waves.get(i).toJson());
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================= JSON 输入 =================
    /**
     * 解析波次 JSON（加载时宽松，规范 §3.4/§3.5）：
     * 单波非法 → 跳过该波并告警；组 type 不在词汇表 → 跳过该组并告警；
     * 顶层不是对象/缺 waves 数组 → 抛 IllegalArgumentException（整体解析失败，由调用方回退）。
     */
    public static LevelWaves fromJson(String json) {
        Object root = new MiniJson(json).parse();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("JSON 顶层必须是对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;
        LevelWaves out = new LevelWaves(obj.containsKey("name") ? String.valueOf(obj.get("name")) : "");

        Object arr = obj.get("waves");
        if (!(arr instanceof List)) {
            throw new IllegalArgumentException("缺少 waves 数组");
        }
        List<?> waveList = (List<?>) arr;
        for (int i = 0; i < waveList.size(); i++) {
            Object w = waveList.get(i);
            if (!(w instanceof Map)) {
                warn("waves[" + i + "] 结构非法，已跳过该波");
                continue;
            }
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> wm = (Map<String, Object>) w;
                out.waves.add(parseWave(i, wm));
            } catch (RuntimeException e) {
                warn("waves[" + i + "] 非法，已跳过该波: " + e.getMessage());
            }
        }
        return out;
    }

    private static Wave parseWave(int index, Map<String, Object> w) {
        String at = "waves[" + index + "]";
        Wave wave = new Wave(w.containsKey("id")
                ? (int) toLong(w.get("id"), at + ".id") : index + 1);
        if (w.containsKey("maxPrepMs") && w.get("maxPrepMs") != null) {
            wave.setMaxPrepMs(toLong(w.get("maxPrepMs"), at + ".maxPrepMs"));
        }
        Object arr = w.get("groups");
        if (!(arr instanceof List)) {
            throw new IllegalArgumentException(at + " 缺少 groups 数组");
        }
        List<?> gl = (List<?>) arr;
        for (int j = 0; j < gl.size(); j++) {
            Object g = gl.get(j);
            if (!(g instanceof Map)) {
                warn(at + ".groups[" + j + "] 结构非法，已跳过该组");
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> gm = (Map<String, Object>) g;
            String type = gm.containsKey("type") ? String.valueOf(gm.get("type")) : "";
            if (!VOCABULARY.containsKey(type)) {
                warn(at + ".groups[" + j + "] 敌人类型不在词汇表（" + type + "），已跳过该组");
                continue;
            }
            long count = toLong(gm.get("count"), at + ".groups[" + j + "].count");
            long interval = toLong(gm.get("intervalMs"), at + ".groups[" + j + "].intervalMs");
            if (count < 1 || interval <= 0) {
                throw new IllegalArgumentException(at + " 存在 count<1 或 intervalMs≤0 的组");
            }
            wave.groups.add(new SpawnGroup(type, (int) count, interval));
        }
        return wave;
    }

    private static long toLong(Object v, String field) {
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException ignored) {
                // fallthrough
            }
        }
        throw new IllegalArgumentException("字段 " + field + " 必须是整数，实际: " + v);
    }

    private static void warn(String msg) {
        System.err.println("[LevelWaves] " + msg);
    }

    // ================= classpath 加载 =================
    /**
     * 从 classpath 读取波次资源（带缓存，规范 §5.1；运行期只读、不抛异常）。
     *
     * @param resource classpath 路径，如 "/maps/&lt;key&gt;/waves.json"
     * @return 解析成功的 LevelWaves；文件缺失/整体解析失败返回 null（调用方回退 GameConfig 全局行为）
     */
    public static LevelWaves loadFromClasspath(String resource) {
        LevelWaves cached = CACHE.get(resource);
        if (cached != null) return cached;
        try (InputStream in = LevelWaves.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            LevelWaves lw = fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            CACHE.put(resource, lw);
            return lw;
        } catch (IOException | RuntimeException e) {
            warn("加载波次失败: " + resource + " -> " + e.getMessage());
            return null;
        }
    }
}
