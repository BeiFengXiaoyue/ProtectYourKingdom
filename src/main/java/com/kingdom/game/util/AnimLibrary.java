package com.kingdom.game.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimLibrary —— 单位动画表仓库（按“类别 + 单位名”管理，天然去重）。
 *
 * 目录约定：
 * - 描述文件：src/main/resources/assets/animations/{enemies,allies,towers}/&lt;单位名&gt;.json
 *   （同类别同名=覆盖修改；不同类别各自独立）；
 * - 帧图片：src/main/resources/assets/{enemies,allies,towers}/&lt;单位名&gt;/&lt;模式&gt;_&lt;序号&gt;.png
 *   （JSON 内帧路径相对 assets/，前缀即该类别子目录）。
 *
 * 编辑器/开发期读写走文件系统；游戏运行期只读用 classpath（AnimTable.loadFromClasspath）。
 */
public final class AnimLibrary {

    public static final String ANIMATIONS_BASE = "assets/animations";

    private AnimLibrary() {
    }

    // ================= 类别 =================
    /** 校验类别名合法 */
    public static boolean isValidKind(String kind) {
        return AnimTable.KIND_ENEMIES.equals(kind)
                || AnimTable.KIND_ALLIES.equals(kind)
                || AnimTable.KIND_TOWERS.equals(kind);
    }

    // ================= 目录定位 =================
    /** 从当前工作目录向上查找 src/main/resources；找不到返回 null */
    public static File findRepoResourcesDir() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, "src/main/resources");
            if (candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
        }
        return null;
    }

    /** 某类别动画表目录：resources/assets/animations/<kind> */
    public static File animationsDir(String kind) {
        File res = findRepoResourcesDir();
        if (res == null || !isValidKind(kind)) return null;
        return new File(new File(res, ANIMATIONS_BASE), kind);
    }

    /** 某类别帧根目录：resources/assets/<kind> */
    public static File framesRoot(String kind) {
        File res = findRepoResourcesDir();
        if (res == null || !isValidKind(kind)) return null;
        return new File(res, "assets/" + kind);
    }

    /** 某类别某单位帧目录：resources/assets/<kind>/<单位名> */
    public static File framesDir(String kind, String unitName) {
        File root = framesRoot(kind);
        if (root == null || unitName == null || unitName.isBlank()) return null;
        return new File(root, unitName);
    }

    /** 相对 assets 的帧路径 → assets 下的文件 */
    public static File resolveFrameFile(String relativePath) {
        File res = findRepoResourcesDir();
        if (res == null || relativePath == null || relativePath.isBlank()) return null;
        File f = new File(res, "assets/" + relativePath);
        return f.isFile() ? f : null;
    }

    // ================= 读取 =================
    /** 是否存在该类别+单位的动画表（先文件系统，后 classpath） */
    public static boolean exists(String kind, String unitName) {
        if (!isValidKind(kind) || unitName == null || unitName.isBlank()) return false;
        File dir = animationsDir(kind);
        if (dir != null && new File(dir, unitName + ".json").isFile()) return true;
        return AnimTable.class.getResourceAsStream(
                "/assets/animations/" + kind + "/" + unitName + ".json") != null;
    }

    /** 列出某类别下已有的单位（.json 文件名） */
    public static List<String> list(String kind) {
        List<String> out = new ArrayList<>();
        File dir = animationsDir(kind);
        if (dir == null) return out;
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".json"));
        if (files == null) return out;
        for (File f : files) {
            String n = f.getName();
            out.add(n.substring(0, n.length() - ".json".length()));
        }
        return out;
    }

    /**
     * 读取某类别+单位的动画表。优先读仓库目录（编辑器编辑态），
     * 其次退回 classpath。不存在返回 null。
     */
    public static AnimTable read(String kind, String unitName) {
        if (!isValidKind(kind) || unitName == null || unitName.isBlank()) return null;
        File dir = animationsDir(kind);
        if (dir != null) {
            File f = new File(dir, unitName + ".json");
            if (f.isFile()) {
                try {
                    return AnimTable.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                } catch (IOException | RuntimeException e) {
                    System.err.println("[AnimLibrary] 读取动画表失败: " + f + " -> " + e.getMessage());
                    return null;
                }
            }
        }
        return AnimTable.loadFromClasspath("/assets/animations/" + kind + "/" + unitName + ".json");
    }

    // ================= 写入 / 删除 =================
    /**
     * 保存动画表：按 table 的 kind + name 覆盖写入
     * animations/&lt;kind&gt;/&lt;name&gt;.json（同类别同名去重）。
     *
     * @return 写出的文件；仓库目录不可用或 kind 非法返回 null
     */
    public static File save(AnimTable table) {
        if (table == null || !isValidKind(table.getKind())
                || table.getName() == null || table.getName().isBlank()) {
            return null;
        }
        File dir = animationsDir(table.getKind());
        if (dir == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        File f = new File(dir, table.getName() + ".json");
        try {
            Files.writeString(f.toPath(), table.toJson(), StandardCharsets.UTF_8);
            return f;
        } catch (IOException e) {
            System.err.println("[AnimLibrary] 保存动画表失败: " + f + " -> " + e.getMessage());
            return null;
        }
    }

    /** 删除某类别+某单位的动画表文件 */
    public static boolean delete(String kind, String unitName) {
        if (!isValidKind(kind) || unitName == null || unitName.isBlank()) return false;
        File dir = animationsDir(kind);
        if (dir == null) return false;
        File f = new File(dir, unitName + ".json");
        return f.delete();
    }
}
