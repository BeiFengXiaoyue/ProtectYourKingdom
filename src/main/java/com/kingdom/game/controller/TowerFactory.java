package com.kingdom.game.controller;

import com.kingdom.game.model.TowerParams;
import com.kingdom.game.model.tower.Tower;

/**
 * TowerFactory —— 塔工厂（SAM）。
 *
 * 与旧的 `BiFunction&lt;Double,Double,Tower&gt;` 相比多了 {@link TowerParams} 参数：
 * 塔的数值不再写死在塔类里，而是由装配处在登记时经 {@link TowerParams} 注入，
 * 接线人员改数值只需改 {@code TowerParamsDefaults}，控制器与塔类都不必动。
 *
 * 装配处可直接用构造方法引用（塔构造签名为 {@code (double, double, TowerParams)}）：
 * {@code controller.registerTower(TowerType.ARROW, spec, params, ArrowTower::new);}
 */
@FunctionalInterface
public interface TowerFactory {

    /** 在 (x,y) 造一座塔，数值取自 params（params 不得为 null） */
    Tower create(double x, double y, TowerParams params);
}
