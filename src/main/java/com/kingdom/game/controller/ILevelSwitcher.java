package com.kingdom.game.controller;

/**
 * ILevelSwitcher（UI → 后端）
 * 运行期切换 / 重开关卡的**动作**契约，由「下一关 / 上一关 / 重开本关」等按钮调用。
 *
 * 与 {@link IWaveStarter} 对称：本接口只放"会改战场与状态"的动作；
 * 关卡列表 / 当前关 / 是否有上下一关等**只读查询**走 {@link IGameStateReader}
 * （该契约明确"不含任何修改操作"，故动作不能挂在它上面）。
 *
 * 失败语义统一：目标关卡不存在或越界时返回 {@code false}，且**状态零变化**。
 */
public interface ILevelSwitcher {

    /** 按 key 切关：成功则清空战场、按新关重置金币/生命/波次并通知 UI；key 不存在或地图数据缺失返回 false */
    boolean switchLevel(String key);

    /** 按序号切关；越界返回 false */
    boolean switchLevelAt(int index);

    /** 跳转到下一关；无下一关返回 false */
    boolean goNextLevel();

    /** 跳转到上一关；无上一关返回 false */
    boolean goPrevLevel();

    /** 重开本关（不切图）：战场与状态重置，地图不变 */
    boolean restartLevel();
}
