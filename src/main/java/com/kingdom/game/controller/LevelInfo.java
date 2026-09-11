package com.kingdom.game.controller;

/**
 * LevelInfo —— 关卡信息（只读值对象）。
 *
 * <p>「关卡 = 一张地图」（本方案一关一张图），由 {@code maps/index.json} 中一个 key 唯一标识。
 * 供 UI 展示关卡列表/当前关使用，详见《多关卡与运行期切图-接口规范》§3.1。
 */
public final class LevelInfo {

    private final int index;
    private final String key;
    private final String name;

    public LevelInfo(int index, String key, String name) {
        this.index = index;
        this.key = key;
        this.name = name;
    }

    /** 关卡序号，从 0 起；= maps/index.json 数组下标（即关卡顺序） */
    public int getIndex() { return index; }

    /** 关卡 key（= 地图 key） */
    public String getKey() { return key; }

    /** 显示名（index.json 的 name，缺省回退 key） */
    public String getName() { return name; }
}
