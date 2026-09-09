package com.kingdom.game.util.map;

import com.kingdom.game.util.json.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TowerSpots —— 塔位标点数据 + JSON 读写 + 游戏启动加载器（工具类）。
 *
 * 与 {@link MapRoute} 同一套模式：标点由 util.editor.TowerSpotEditorTool 手工标注并
 * 写入 当前地图 src/main/resources/maps/&lt;key&gt;/spots.json（经 util.map.MapLibrary），
 * 游戏启动时经 {@link #loadFromClasspath(String)}（MapLibrary.readSpotsFromClasspath）读取，
 * 建塔菜单/塔位渲染随之生效。
 *
 * 坐标语义与游戏一致：像素坐标、点位为"塔位中心点"。
 *
 * JSON schema（仅支持本子集，复用 util.json.MiniJson 解析）：
 * <pre>
 * {
 *   "name": "tower_spots",
 *   "width": 700,
 *   "height": 600,
 *   "spots": [ {"x":150,"y":120}, {"x":420,"y":90}, ... ]
 * }
 * </pre>
 */
public final class TowerSpots {

    private final String name;
    private final double width;
    private final double height;
    private final double[] xs;
    private final double[] ys;

    private TowerSpots(String name, double width, double height, double[] xs, double[] ys) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height 必须为正数");
        }
        if (xs == null || ys == null || xs.length != ys.length) {
            throw new IllegalArgumentException("xs/ys 不能为空且长度必须一致");
        }
        this.name = name;
        this.width = width;
        this.height = height;
        this.xs = xs;
        this.ys = ys;
    }

    // ================= 构造 =================
    public static TowerSpots of(String name, double width, double height, double[] xs, double[] ys) {
        return new TowerSpots(name, width, height, xs, ys);
    }

    public static TowerSpots of(double width, double height, double[] xs, double[] ys) {
        return new TowerSpots("tower_spots", width, height, xs, ys);
    }

    // ================= Getter =================
    public String getName() { return name; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int spotCount() { return xs.length; }
    public double[] getXs() { return xs.clone(); }
    public double[] getYs() { return ys.clone(); }

    // ================= JSON 输出 =================
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escape(name)).append("\"");
        sb.append(",\n  \"width\": ").append(num(width));
        sb.append(",\n  \"height\": ").append(num(height));
        sb.append(",\n  \"spots\": [");
        for (int i = 0; i < xs.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\n    {\"x\":").append(num(xs[i]))
              .append(",\"y\":").append(num(ys[i])).append("}");
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    private static String escape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String num(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    // ================= JSON 输入 =================
    public static TowerSpots fromJson(String json) {
        Object root = new MiniJson(json).parse();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("JSON 顶层必须是对象");
        }
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> obj = (java.util.Map<String, Object>) root;

        double width = toDouble(obj.get("width"), "width");
        double height = toDouble(obj.get("height"), "height");
        String name = obj.containsKey("name") ? String.valueOf(obj.get("name")) : "tower_spots";

        Object arr = obj.get("spots");
        if (!(arr instanceof List)) {
            throw new IllegalArgumentException("缺少 spots 数组");
        }
        List<Object> spotList = (List<Object>) arr;
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < spotList.size(); i++) {
            Object p = spotList.get(i);
            if (!(p instanceof Map)) {
                throw new IllegalArgumentException("spots[" + i + "] 必须是 {\"x\":..,\"y\":..}");
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> pm = (java.util.Map<String, Object>) p;
            pts.add(new double[]{toDouble(pm.get("x"), "spots[" + i + "].x"),
                    toDouble(pm.get("y"), "spots[" + i + "].y")});
        }
        double[] xs = new double[pts.size()];
        double[] ys = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            xs[i] = pts.get(i)[0];
            ys[i] = pts.get(i)[1];
        }
        return new TowerSpots(name, width, height, xs, ys);
    }

    private static double toDouble(Object v, String field) {
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                // fallthrough
            }
        }
        throw new IllegalArgumentException("字段 " + field + " 必须是数字，实际: " + v);
    }

    // ================= classpath 加载 =================
    /**
     * 从 classpath 读取塔位资源。
     *
     * @param resource classpath 路径，如 "/maps/&lt;key&gt;/spots.json"
     * @return 解析成功的 TowerSpots；文件缺失/解析失败返回 null
     */
    public static TowerSpots loadFromClasspath(String resource) {
        try (InputStream in = TowerSpots.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IOException | RuntimeException e) {
            System.err.println("[TowerSpots] 加载塔位失败: " + resource + " -> " + e.getMessage());
            return null;
        }
    }
}
