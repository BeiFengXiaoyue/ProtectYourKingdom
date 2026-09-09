package com.kingdom.game.util;

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
 * MapLibrary —— 地图分包仓库（maps/index.json 注册表 + 每图一个目录）。
 *
 * 目录约定：
 * <pre>
 * src/main/resources/maps/
 * ├── index.json                     { "maps":[ {"key":"default","name":"默认地图","image":"map.png"} ] }
 * └── &lt;key&gt;/
 *     ├── &lt;image&gt;                底图
 *     ├── path.json                 敌人路径（MapRoute 结构）
 *     └── spots.json                塔位（TowerSpots 结构）
 * </pre>
 * - index 是“已有地图”的唯一事实来源（列表/顺序/描述）；
 * - 编辑器/开发期读写走文件系统；游戏运行期只读用 classpath 版。
 */
public final class MapLibrary {

    public static final String MAPS_SUBDIR = "maps";
    public static final String INDEX_FILE = "index.json";
    public static final String PATH_FILE = "path.json";
    public static final String SPOTS_FILE = "spots.json";

    /** 一个地图的注册条目 */
    public static final class MapEntry {
        private final String key;
        private final String name;
        private final String image;

        MapEntry(String key, String name, String image) {
            this.key = key;
            this.name = name == null || name.isBlank() ? key : name;
            this.image = image == null || image.isBlank() ? "map.png" : image;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public String getImage() { return image; }

        String toJsonEntry() {
            return "    { \"key\": \"" + esc(key) + "\", \"name\": \"" + esc(name)
                    + "\", \"image\": \"" + esc(image) + "\" }";
        }

        static MapEntry from(Map<String, Object> obj) {
            String k = obj.containsKey("key") ? String.valueOf(obj.get("key")) : "";
            String n = obj.containsKey("name") ? String.valueOf(obj.get("name")) : k;
            String img = obj.containsKey("image") ? String.valueOf(obj.get("image")) : "map.png";
            return new MapEntry(k, n, img);
        }
    }

    private MapLibrary() {
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ================= 目录 =================
    public static File findRepoResourcesDir() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, "src/main/resources");
            if (candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
        }
        return null;
    }

    public static File mapsDir() {
        File res = findRepoResourcesDir();
        return res == null ? null : new File(res, MAPS_SUBDIR);
    }

    public static File mapDir(String key) {
        File maps = mapsDir();
        if (maps == null || key == null || key.isBlank()) return null;
        return new File(maps, key);
    }

    public static File pathFile(String key) {
        File d = mapDir(key);
        return d == null ? null : new File(d, PATH_FILE);
    }

    public static File spotsFile(String key) {
        File d = mapDir(key);
        return d == null ? null : new File(d, SPOTS_FILE);
    }

    // ================= index 读写 =================
    private static List<MapEntry> parseIndex(String json) {
        List<MapEntry> out = new ArrayList<>();
        Object root = new MapRoute.MiniJson(json).parse();
        if (!(root instanceof Map)) return out;
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;
        Object maps = obj.get("maps");
        if (maps instanceof List) {
            for (Object m : (List<?>) maps) {
                if (m instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mm = (Map<String, Object>) m;
                    out.add(MapEntry.from(mm));
                }
            }
        }
        return out;
    }

    private static String toIndexJson(List<MapEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"maps\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(entries.get(i).toJsonEntry());
        }
        sb.append("\n  ]\n}\n");
        return sb.toString();
    }

    /** 读 index（优先仓库文件，退回 classpath） */
    public static List<MapEntry> listMaps() {
        File maps = mapsDir();
        if (maps != null) {
            File f = new File(maps, INDEX_FILE);
            if (f.isFile()) {
                try {
                    return parseIndex(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                } catch (IOException | RuntimeException e) {
                    System.err.println("[MapLibrary] 读取地图索引失败: " + f + " -> " + e.getMessage());
                }
            }
        }
        try (java.io.InputStream in = MapLibrary.class.getResourceAsStream("/maps/" + INDEX_FILE)) {
            if (in == null) return new ArrayList<>();
            return parseIndex(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static boolean writeIndex(List<MapEntry> entries) {
        File maps = mapsDir();
        if (maps == null) return false;
        if (!maps.exists() && !maps.mkdirs()) return false;
        try {
            Files.writeString(new File(maps, INDEX_FILE).toPath(), toIndexJson(entries),
                    StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("[MapLibrary] 写入地图索引失败: " + e.getMessage());
            return false;
        }
    }

    public static boolean exists(String key) {
        if (key == null || key.isBlank()) return false;
        for (MapEntry e : listMaps()) {
            if (e.getKey().equals(key)) {
                File d = mapDir(key);
                return d != null && d.isDirectory();
            }
        }
        return false;
    }

    public static MapEntry getEntry(String key) {
        for (MapEntry e : listMaps()) {
            if (e.getKey().equals(key)) return e;
        }
        return null;
    }

    /** 默认地图 key：index 第一条，否则 "default" */
    public static String defaultKey() {
        List<MapEntry> entries = listMaps();
        return entries.isEmpty() ? "default" : entries.get(0).getKey();
    }

    /** 新增/覆盖一张地图：建目录+拷底图+更新 index（同名 key=覆盖） */
    public static MapEntry createMap(String key, String name, File imageSource) {
        if (key == null || key.isBlank()) return null;
        File dir = mapDir(key);
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        String imageName = imageSource == null ? "map.png" : imageSource.getName();
        File dst = new File(dir, imageName);
        if (imageSource != null) {
            try {
                Files.copy(imageSource.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("[MapLibrary] 拷贝底图失败: " + e.getMessage());
                return null;
            }
        }
        MapEntry entry = new MapEntry(key, name, imageName);
        List<MapEntry> entries = listMaps();
        boolean replaced = false;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(key)) {
                entries.set(i, entry);
                replaced = true;
                break;
            }
        }
        if (!replaced) entries.add(entry);
        writeIndex(entries);
        return entry;
    }

    /** 删除一张地图（目录 + index 条目） */
    public static boolean deleteMap(String key) {
        List<MapEntry> entries = listMaps();
        boolean removed = entries.removeIf(e -> e.getKey().equals(key));
        if (removed) writeIndex(entries);
        File dir = mapDir(key);
        if (dir != null && dir.isDirectory()) {
            return deleteRecursively(dir);
        }
        return removed;
    }

    private static boolean deleteRecursively(File f) {
        if (f == null || !f.exists()) return false;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursively(c);
            }
        }
        return f.delete();
    }

    // ================= 文件系统读写（编辑器用）=================
    public static MapRoute readPath(String key) {
        File f = pathFile(key);
        if (f == null || !f.isFile()) return null;
        try {
            return MapRoute.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            System.err.println("[MapLibrary] 读取路径失败: " + f + " -> " + e.getMessage());
            return null;
        }
    }

    public static boolean writePath(String key, MapRoute route) {
        File f = pathFile(key);
        if (f == null) return false;
        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) return false;
        try {
            Files.writeString(f.toPath(), route.toJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("[MapLibrary] 写入路径失败: " + e.getMessage());
            return false;
        }
    }

    public static TowerSpots readSpots(String key) {
        File f = spotsFile(key);
        if (f == null || !f.isFile()) return null;
        try {
            return TowerSpots.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            System.err.println("[MapLibrary] 读取塔位失败: " + f + " -> " + e.getMessage());
            return null;
        }
    }

    public static boolean writeSpots(String key, TowerSpots spots) {
        File f = spotsFile(key);
        if (f == null) return false;
        if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) return false;
        try {
            Files.writeString(f.toPath(), spots.toJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("[MapLibrary] 写入塔位失败: " + e.getMessage());
            return false;
        }
    }

    // ================= classpath 只读（运行期用）=================
    /** 运行期从 classpath 读默认 index（供 GameConfig 选图） */
    public static List<MapEntry> listMapsFromClasspath() {
        try (java.io.InputStream in = MapLibrary.class.getResourceAsStream("/maps/" + INDEX_FILE)) {
            if (in == null) return new ArrayList<>();
            return parseIndex(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public static MapRoute readPathFromClasspath(String key) {
        return MapRoute.loadFromClasspath("/maps/" + key + "/" + PATH_FILE);
    }

    public static TowerSpots readSpotsFromClasspath(String key) {
        return TowerSpots.loadFromClasspath("/maps/" + key + "/" + SPOTS_FILE);
    }

    /** 供界面组合用 */
    public static String entryLabel(MapEntry e) {
        return e == null ? "" : e.getKey() + "（" + e.getName() + "）";
    }
}
