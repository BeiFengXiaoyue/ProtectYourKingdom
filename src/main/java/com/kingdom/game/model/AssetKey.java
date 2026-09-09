package com.kingdom.game.model;

/**
 * AssetKey —— 贴图资源 key（素材词汇表）。
 *
 * 素材插入通道契约：
 * 1. 素材收集者按 {@link #getFileName()} 命名的文件放入 src/main/resources/assets/；
 * 2. 实体 render 用 {@link com.kingdom.game.util.asset.Assets#get(AssetKey)} 取图，
 *    取不到（未提供素材）时回退为色块/形状，游戏不受阻。
 */
public enum AssetKey {
    NORMAL_ENEMY("normal_enemy.png"),
    FAST_ENEMY("fast_enemy.png"),
    TANK_ENEMY("tank_enemy.png"),
    BOSS_ENEMY("boss_enemy.png"),
    SOLDIER("soldier.png"),
    ARROW_TOWER("arrow_tower.png"),
    CANNON_TOWER("cannon_tower.png"),
    BARRACK("barrack.png"),
    ARROW("arrow.png"),
    BOMB("bomb.png");

    private final String fileName;

    AssetKey(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
