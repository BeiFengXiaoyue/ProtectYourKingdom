package com.kingdom.game.util.balance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * BalanceLibrary —— 数值配置仓库（config/balance.json 的文件系统读写入口）。
 *
 * 双轨职责（与 MapLibrary 同范式）：
 * - 开发期/编辑器：read() 优先读仓库文件 src/main/resources/config/balance.json，
 *   找不到时退回 classpath；write() 直接写仓库文件。
 * - 运行期：readFromClasspath() 只读 classpath 版（Maven 自动把 resources 打进 classpath），
 *   供 GameConfig 构造期调用，缺失返回 null。
 *
 * 两条轨指向同一个文件：工具改仓库文件 → Maven 构建后进入 classpath → 游戏重启读取。
 */
public final class BalanceLibrary {

    public static final String BALANCE_SUBDIR = "config";
    public static final String BALANCE_FILE = "balance.json";

    private BalanceLibrary() { }

    // ================= 目录定位 =================

    /**
     * 从 user.dir 向上查找 src/main/resources 目录（同 MapLibrary.findRepoResourcesDir 规则）。
     * @return 找到的 resources 目录；找不到返回 null
     */
    public static File findRepoResourcesDir() {
        File dir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < 8 && dir != null; i++) {
            File candidate = new File(dir, "src/main/resources");
            if (candidate.isDirectory()) return candidate;
            dir = dir.getParentFile();
        }
        return null;
    }

    /** 仓库中的 balance.json 文件（src/main/resources/config/balance.json）；找不到 resources 目录返回 null */
    public static File balanceFile() {
        File res = findRepoResourcesDir();
        if (res == null) return null;
        return new File(new File(res, BALANCE_SUBDIR), BALANCE_FILE);
    }

    // ================= 读写（开发期/编辑器用）=================

    /**
     * 读取数值配置：优先仓库文件，退回 classpath。
     * 供工具/编辑器调用；文件缺失/解析失败 → 返回 null（调用方应回退 BalanceTable.defaults()）。
     */
    public static BalanceTable read() {
        File f = balanceFile();
        if (f != null && f.isFile()) {
            try {
                String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                return BalanceTable.fromJson(json);
            } catch (IOException | RuntimeException e) {
                System.err.println("[BalanceLibrary] 读取仓库 balance.json 失败: " + f + " -> " + e.getMessage() + " → 退回 classpath");
            }
        }
        return readFromClasspath();
    }

    /**
     * 写入数值配置到仓库文件（mkdirs + Files.writeString）。
     * @return 写入成功返回 true；失败返回 false 并打印警告
     */
    public static boolean write(BalanceTable table) {
        if (table == null) return false;
        File f = balanceFile();
        if (f == null) {
            System.err.println("[BalanceLibrary] 写入失败：未找到 src/main/resources 目录");
            return false;
        }
        File parent = f.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            System.err.println("[BalanceLibrary] 写入失败：无法创建目录 " + parent);
            return false;
        }
        try {
            Files.writeString(f.toPath(), table.toJson(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("[BalanceLibrary] 写入 balance.json 失败: " + e.getMessage());
            return false;
        }
    }

    // ================= classpath 只读（运行期 GameConfig 用）=================

    /**
     * 运行期从 classpath 读取 /config/balance.json。
     * 缺失/解析失败 → 返回 null（GameConfig 应回退 BalanceTable.defaults()）。
     */
    public static BalanceTable readFromClasspath() {
        return BalanceTable.loadFromClasspath();
    }
}
