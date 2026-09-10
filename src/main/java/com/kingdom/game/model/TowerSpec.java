package com.kingdom.game.model;

/**
 * TowerSpec —— 塔目录条目（建塔菜单的数据源）。
 *
 * 数值/名称不写死在 TowerType 枚举里，而是以本条目录数据 + 工厂注册的形式提供，
 * 从而支持"逐步开发"：最开始只有箭塔（ARROW）（现已有三族三级共 9 个），后续加塔只需
 * ① 新建具体 Tower 子类；
 * ② 若需新的类型 id，在 TowerType 追加一个常量；
 * ③ 在装配处 registerTower(type, spec, factory)（1 级塔，进菜单）或
 *    registerUpgradeFactory(type, factory)（升级级塔，只进工厂表）。
 */
public final class TowerSpec {
    private final TowerType type;
    private final String displayName;
    private final int cost;

    public TowerSpec(TowerType type, String displayName, int cost) {
        this.type = type;
        this.displayName = displayName;
        this.cost = cost;
    }

    public TowerType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public int getCost() { return cost; }
}
