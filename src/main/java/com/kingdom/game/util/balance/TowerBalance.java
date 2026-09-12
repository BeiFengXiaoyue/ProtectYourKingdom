package com.kingdom.game.util.balance;

import com.kingdom.game.util.json.MiniJson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TowerBalance —— 塔数值配置仓库（config/tower.json 的读取入口）。
 *
 * 用法（塔类构造函数中）：
 *   this.baseAttackDamage = TowerBalance.getInt("arrowL1", "damage", 25);
 *   this.attackRange = TowerBalance.getDouble("arrowL1", "range", 120);
 *
 * 改 tower.json 后重启游戏生效（同 balance.json 契约）。
 * 字段缺失/文件缺失 → 返回传入的硬编码默认值，不抛异常。
 */
public final class TowerBalance {

    private static final String SUBDIR = "config";
    private static final String FILE = "tower.json";

    /** 类加载时读取一次并缓存；缺失/解析失败 → 空 Map，全部走默认值 */
    private static final Map<String, Map<String, Object>> CACHE = load();

    private TowerBalance() { }

    // ================= 对外取值 =================

    /** 取 int 型塔数值；缺失/类型错 → 返回 defaultValue */
    public static int getInt(String towerId, String field, int defaultValue) {
        Object v = getRaw(towerId, field);
        if (v instanceof Number) return ((Number) v).intValue();
        return defaultValue;
    }

    /** 取 double 型塔数值；缺失/类型错 → 返回 defaultValue */
    public static double getDouble(String towerId, String field, double defaultValue) {
        Object v = getRaw(towerId, field);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return defaultValue;
    }

    private static Object getRaw(String towerId, String field) {
        Map<String, Object> tower = CACHE.get(towerId);
        if (tower == null) return null;
        return tower.get(field);
    }

    // ================= 加载 =================

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> load() {
        // 1. 优先仓库文件
        File res = BalanceLibrary.findRepoResourcesDir();
        if (res != null) {
            File f = new File(new File(res, SUBDIR), FILE);
            if (f.isFile()) {
                try {
                    String json = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    Object root = new MiniJson(json).parse();
                    if (root instanceof Map) {
                        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> e : ((Map<String, Object>) root).entrySet()) {
                            if (e.getValue() instanceof Map) {
                                result.put(e.getKey(), (Map<String, Object>) e.getValue());
                            }
                        }
                        return result;
                    }
                } catch (IOException | RuntimeException ex) {
                    System.err.println("[TowerBalance] 读取仓库 tower.json 失败: " + ex.getMessage() + " → 退回 classpath");
                }
            }
        }
        // 2. 退回 classpath
        try (InputStream in = TowerBalance.class.getResourceAsStream("/config/tower.json")) {
            if (in == null) {
                System.err.println("[TowerBalance] 未找到 /config/tower.json → 全部走硬编码默认");
                return new LinkedHashMap<>();
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Object root = new MiniJson(json).parse();
            if (root instanceof Map) {
                Map<String, Map<String, Object>> result = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : ((Map<String, Object>) root).entrySet()) {
                    if (e.getValue() instanceof Map) {
                        result.put(e.getKey(), (Map<String, Object>) e.getValue());
                    }
                }
                return result;
            }
        } catch (IOException | RuntimeException ex) {
            System.err.println("[TowerBalance] 加载 /config/tower.json 失败: " + ex.getMessage() + " → 全部走硬编码默认");
        }
        return new LinkedHashMap<>();
    }
}
