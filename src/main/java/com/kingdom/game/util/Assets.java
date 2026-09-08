package com.kingdom.game.util;

import com.kingdom.game.model.AssetKey;
import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Assets —— 贴图仓库（素材插入通道，工具类）。
 *
 * 约定：
 * - 贴图文件由素材收集者放到 src/main/resources/assets/ 下，文件名 = AssetKey 的文件名；
 * - Maven 默认把 src/main/resources 打进 classpath，因此以 /assets/&lt;文件名&gt; 访问；
 * - get(...) 懒加载并缓存；文件缺失返回 null，实体自行回退为色块/形状，游戏不阻塞。
 */
public final class Assets {

    private static final Map<AssetKey, Image> CACHE = new EnumMap<>(AssetKey.class);

    private Assets() {
    }

    /** 取贴图；不存在/加载失败返回 null */
    public static Image get(AssetKey key) {
        if (key == null) return null;
        Image cached = CACHE.get(key);
        if (cached != null) return cached;
        Image loaded = load(key);
        if (loaded != null) CACHE.put(key, loaded);
        return loaded;
    }

    /** 预加载全部已存在的贴图（可在启动阶段调用一次） */
    public static void preload() {
        for (AssetKey key : AssetKey.values()) {
            get(key);
        }
    }

    private static Image load(AssetKey key) {
        String resource = "/assets/" + key.getFileName();
        try (InputStream in = Assets.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            return new Image(in);
        } catch (Exception e) {
            return null;
        }
    }
}
