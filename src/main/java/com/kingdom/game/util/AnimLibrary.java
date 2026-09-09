package com.kingdom.game.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * AnimLibrary —— 敌人动画表仓库（按“敌人名”管理，天然去重）。
 *
 * 目录约定：
 * - 描述文件：src/main/resources/assets/animations/&lt;敌人名&gt;.json（一个敌人一个文件，同名=覆盖修改）；
 * - 帧图片：src/main/resources/assets/enemies/&lt;敌人名&gt;/&lt;模式&gt;_&lt;序号&gt;.png（编辑器自动拷贝）。
 *
 * 编辑器/开发期读写走文件系统；游戏运行期只读用 classpath（AnimTable.loadFromClasspath）。
 */
public final class AnimLibrary {

    public static final String ANIMATIONS_SUBDIR = "assets/animations";
    public static final String ENEMY_FRAMES_SUBDIR = "assets/enemies";

    private AnimLibrary() {
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

    public static File animationsDir() {
        File res = findRepoResourcesDir();
        return res == null ? null : new File(res, ANIMATIONS_SUBDIR);
    }

    public static File enemyFramesDir(String enemyName) {
        File res = findRepoResourcesDir();
        if (res == null || enemyName == null || enemyName.isBlank()) return null;
        return new File(new File(res, ENEMY_FRAMES_SUBDIR), enemyName);
    }

    /** 相对 assets 的帧路径 → assets 下的文件 */
    public static File resolveFrameFile(String relativePath) {
        File res = findRepoResourcesDir();
        if (res == null || relativePath == null || relativePath.isBlank()) return null;
        File f = new File(res, "assets/" + relativePath);
        return f.isFile() ? f : null;
    }

    // ================= 读取 =================
    /** 是否存在该敌人的动画表（先文件系统，后 classpath） */
    public static boolean exists(String enemyName) {
        if (enemyName == null || enemyName.isBlank()) return false;
        File dir = animationsDir();
        if (dir != null && new File(dir, enemyName + ".json").isFile()) return true;
        return AnimTable.class.getResourceAsStream("/assets/animations/" + enemyName + ".json") != null;
    }

    /** 列出仓库 animations 目录下已有的敌人（.json 文件名） */
    public static List<String> listEnemies() {
        List<String> out = new ArrayList<>();
        File dir = animationsDir();
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
     * 读取某敌人的动画表。优先读仓库 animations 下的文件（编辑器编辑态），
     * 其次退回 classpath。不存在返回 null。
     */
    public static AnimTable read(String enemyName) {
        if (enemyName == null || enemyName.isBlank()) return null;
        File dir = animationsDir();
        if (dir != null) {
            File f = new File(dir, enemyName + ".json");
            if (f.isFile()) {
                try {
                    return AnimTable.fromJson(Files.readString(f.toPath(), StandardCharsets.UTF_8));
                } catch (IOException | RuntimeException e) {
                    System.err.println("[AnimLibrary] 读取动画表失败: " + f + " -> " + e.getMessage());
                    return null;
                }
            }
        }
        return AnimTable.loadFromClasspath("/assets/animations/" + enemyName + ".json");
    }

    // ================= 写入 / 删除 =================
    /**
     * 保存动画表：按 table.getName() 覆盖写入 animations/&lt;名字&gt;.json（同名去重）。
     *
     * @return 写出的文件；仓库目录不可用返回 null
     */
    public static File save(AnimTable table) {
        if (table == null || table.getName() == null || table.getName().isBlank()) return null;
        File dir = animationsDir();
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

    /** 删除某敌人的动画表文件 */
    public static boolean delete(String enemyName) {
        if (enemyName == null || enemyName.isBlank()) return false;
        File dir = animationsDir();
        if (dir == null) return false;
        File f = new File(dir, enemyName + ".json");
        return f.delete();
    }
}
