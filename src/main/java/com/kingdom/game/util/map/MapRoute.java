package com.kingdom.game.util.map;

import com.kingdom.game.util.json.MiniJson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MapRoute —— 路线数据 + JSON 读写 + 游戏启动加载器（工具类）。
 *
 * 数据语义与游戏契约一致：
 * - 坐标 = 画布像素坐标、路径点即“中心点”；
 * - 首点 = 敌人出生点，末点 = 漏怪终点；
 * - 应用到游戏只需 config.setViewSize(w,h).setPath(xs,ys)，渲染/出生/移动/禁塔区自动跟随。
 *
 * JSON schema（按地图分包：写入 src/main/resources/maps/&lt;key&gt;/path.json）：
 * <pre>
 * {
 *   "name": "default_path",
 *   "image": "map.png",        // 可选：参考底图文件名（工具记录用，游戏暂不加载）
 *   "width": 900,
 *   "height": 560,
 *   "points": [ {"x":60,"y":160}, {"x":300,"y":160}, ... ]
 * }
 * </pre>
 * 无第三方 JSON 库，内部使用仅支持本 schema 子集的极简读写器。
 */
public final class MapRoute {

    private final String name;
    private final String image;   // 可空：参考底图文件名（非游戏运行时加载）
    private final double width;
    private final double height;
    private final double[] xs;
    private final double[] ys;

    private MapRoute(String name, String image, double width, double height, double[] xs, double[] ys) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height 必须为正数");
        }
        if (xs == null || ys == null || xs.length < 2 || xs.length != ys.length) {
            throw new IllegalArgumentException("路线至少需要 2 个点，且 xs/ys 长度必须一致");
        }
        this.name = name;
        this.image = image;
        this.width = width;
        this.height = height;
        this.xs = xs;
        this.ys = ys;
    }

    // ================= 构造 =================
    public static MapRoute of(String name, String image, double width, double height,
                              double[] xs, double[] ys) {
        return new MapRoute(name, image, width, height, xs, ys);
    }

    public static MapRoute of(double width, double height, double[] xs, double[] ys) {
        return new MapRoute("default_path", null, width, height, xs, ys);
    }

    // ================= Getter =================
    public String getName() { return name; }
    public String getImage() { return image; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int pointCount() { return xs.length; }
    public double[] getXs() { return xs.clone(); }
    public double[] getYs() { return ys.clone(); }

    // ================= JSON 输出 =================
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("  \"name\": \"").append(escape(name)).append("\"");
        if (image != null) {
            sb.append(",\n  \"image\": \"").append(escape(image)).append("\"");
        }
        sb.append(",\n  \"width\": ").append(num(width));
        sb.append(",\n  \"height\": ").append(num(height));
        sb.append(",\n  \"points\": [");
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
    public static MapRoute fromJson(String json) {
        Object root = new MiniJson(json).parse();
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("JSON 顶层必须是对象");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;

        double width = toDouble(obj.get("width"), "width");
        double height = toDouble(obj.get("height"), "height");
        String name = obj.containsKey("name") ? String.valueOf(obj.get("name")) : "default_path";
        String image = obj.containsKey("image") && obj.get("image") != null
                ? String.valueOf(obj.get("image")) : null;

        Object pts = obj.get("points");
        if (!(pts instanceof List)) {
            throw new IllegalArgumentException("缺少 points 数组");
        }
        List<Object> pointList = (List<Object>) pts;
        int n = pointList.size();
        if (n < 2) {
            throw new IllegalArgumentException("路线至少需要 2 个点");
        }
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            Object p = pointList.get(i);
            if (!(p instanceof Map)) {
                throw new IllegalArgumentException("points[" + i + "] 必须是 {\"x\":..,\"y\":..}");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> pm = (Map<String, Object>) p;
            xs[i] = toDouble(pm.get("x"), "points[" + i + "].x");
            ys[i] = toDouble(pm.get("y"), "points[" + i + "].y");
        }
        return new MapRoute(name, image, width, height, xs, ys);
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
     * 从 classpath 读取路线资源。
     *
     * @param resource classpath 路径，如 "/maps/&lt;key&gt;/path.json"
     * @return 解析成功的 MapRoute；文件缺失/解析失败返回 null
     */
    public static MapRoute loadFromClasspath(String resource) {
        try (InputStream in = MapRoute.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return fromJson(json);
        } catch (IOException | RuntimeException e) {
            System.err.println("[MapRoute] 加载路线失败: " + resource + " -> " + e.getMessage());
            return null;
        }
    }

}
