package com.kingdom.game.model.tower;

import com.kingdom.game.model.TowerSpec;

/**
 * ITowerUpgrade —— 塔升级链查询接口（设计稿：每级 = 独立类型/类）。
 *
 * 交互层（UI 详情面板）只依赖本接口得知"能否升级/下一级名称与造价"，不 import 具体塔类、不做 instanceof 分派。
 * GameController 负责"原位替换"（删旧塔、同坐标建下一级塔），本次由架构师 A 落地。
 */
public interface ITowerUpgrade {

    /**
     * 更高一级塔的目录条目（type/显示名/造价）。
     *
     * @return 非 null = 可升级（作为升级费/下一级信息来源）；null = 已满级
     */
    TowerSpec getNextLevelSpec();

    /** 是否已满级（无更高一级） */
    boolean isMaxLevel();

    /**
     * 本塔等级（1 / 2 / 3）。
     *
     * 等级由**注入的数值**决定（{@code TowerParams.level}，默认值见 {@code TowerParamsDefaults}），
     * 由 {@code Tower} 基类统一实现（{@code return params.getLevel()}）。
     * 每级仍是独立塔类（见《防御塔子系统说明》§8），但等级不再以类常量声明。
     */
    int getLevel();
}
