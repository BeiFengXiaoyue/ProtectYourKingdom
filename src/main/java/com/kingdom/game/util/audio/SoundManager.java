package com.kingdom.game.util.audio;

import com.kingdom.game.controller.ICombatSound;
import com.kingdom.game.controller.IEndSound;
import com.kingdom.game.controller.ITowerSound;
import com.kingdom.game.controller.IUnitSound;
import com.kingdom.game.controller.IWaveSound;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;
import javafx.scene.media.AudioClip;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * SoundManager —— 音效管理器（素材/音效负责人 E）。
 *
 * 一次性实现全部 5 个音效接口（IWaveSound/ITowerSound/IUnitSound/ICombatSound/IEndSound），
 * 在 Main 中注入 controller 的 5 个 setter 即可整体生效；未注入前 FxNop 兜底。
 *
 * 设计约定：
 * - 音频文件位于 classpath:/assets/sounds/，懒加载 + 缓存（AudioClip 适合短音效高频触发）；
 * - 任何文件缺失/损坏只记录一次日志并保持静默，绝不抛异常阻塞游戏（NFR-E5 容错）；
 * - 单位死亡随机播 die_1~4 并随机变调（FR-E7）；投射物命中按追踪/定点区分箭矢与炸弹（FR-E8）；
 * - 战斗类音效带节流阀（{@link #playThrottled}）：怪物扎堆死亡时同效不重叠刷屏，超频直接丢弃。
 *
 * 交互侧（D）扩展调用入口（详见 docs/音效扩展方法说明.md）：
 * - {@link #getInstance()} 获取单例；
 * - {@link #onLifeLost()} 漏怪扣生命时（已在 view/HUD 接线）。
 */
public final class SoundManager implements IWaveSound, ITowerSound, IUnitSound, ICombatSound, IEndSound {

    private static final String SOUND_DIR = "/assets/sounds/";
    private static final double DEFAULT_VOLUME = 0.35;
    /** 默认节流间隔(ms)：同类音效两次播放的最小间距，超出触发次数直接丢弃 */
    private static final long DEFAULT_THROTTLE_MS = 120;

    /** 全部音效清单：构造期一次性预加载，杜绝首次播放时在界面线程做磁盘 IO（Day05 线程纪律）。
     *  注意：必须声明在 INSTANCE 之前——单例构造期要遍历本清单。 */
    private static final String[] ALL_SOUNDS = {
            "wave_start.wav", "tower_place.wav", "tower_upgrade.wav", "tower_sell.wav",
            "unit_spawn.wav", "arrow_release.wav", "arrow_hit.wav", "bomb_hit.wav",
            "melee_attack.wav", "die_1.wav", "die_2.wav", "die_3.wav", "die_4.wav",
            "boss_stomp.wav", "life_lost.wav", "gold_insufficient.wav", "win.wav", "lose.mp3"
    };

    private static final SoundManager INSTANCE = new SoundManager();

    /** 单例：交互侧无法经 controller 注入时，可直接 getInstance() 调用扩展音效方法 */
    public static SoundManager getInstance() {
        return INSTANCE;
    }

    private final Map<String, AudioClip> cache = new HashMap<>();
    private final Map<String, Long> lastPlayAt = new HashMap<>();
    private final Random random = new Random();

    private SoundManager() {
        for (String file : ALL_SOUNDS) {
            clip(file);   // 启动期预加载进缓存；缺失文件只告警，不阻断
        }
    }

    // ================= 播放基础 =================
    /** 取缓存的 AudioClip；未加载过则尝试加载。失败返回 null（只警告一次） */
    private AudioClip clip(String file) {
        if (cache.containsKey(file)) return cache.get(file);
        AudioClip loaded = null;
        try {
            URL url = SoundManager.class.getResource(SOUND_DIR + file);
            if (url != null) {
                loaded = new AudioClip(url.toExternalForm());
                loaded.setVolume(DEFAULT_VOLUME);
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] 音效加载失败: " + file + " -> " + e.getMessage());
        }
        if (loaded == null) {
            System.err.println("[SoundManager] 缺少音效文件: " + SOUND_DIR + file + "（静默跳过）");
        }
        cache.put(file, loaded);
        return loaded;
    }

    /** 无节流播放（一次性事件：波次/塔操作/胜负） */
    private void play(String file) {
        playInternal(file, 1.0);
    }

    /** 带节流播放：距上次播放不足 gapMs 时直接丢弃（高频战斗事件防吵） */
    private void playThrottled(String file, long gapMs) {
        long now = System.currentTimeMillis();
        Long last = lastPlayAt.get(file);
        if (last != null && now - last < gapMs) return;
        lastPlayAt.put(file, now);
        playInternal(file, 1.0);
    }

    /** 带节流 + 随机变调（baseRate×0.9~1.1），用于死亡等高重复音效降低听感疲劳 */
    private void playThrottledPitched(String file, long gapMs, double baseRate) {
        long now = System.currentTimeMillis();
        Long last = lastPlayAt.get(file);
        if (last != null && now - last < gapMs) return;
        lastPlayAt.put(file, now);
        playInternal(file, baseRate * (0.9 + random.nextDouble() * 0.2));
    }

    private void playInternal(String file, double rate) {
        playInternal(file, rate, DEFAULT_VOLUME);
    }

    private void playInternal(String file, double rate, double volume) {
        try {
            AudioClip clip = clip(file);
            if (clip != null) {
                clip.setRate(rate);
                clip.setVolume(volume);
                clip.play();
            }
        } catch (Exception e) {
            System.err.println("[SoundManager] 音效播放失败: " + file + " -> " + e.getMessage());
        }
    }

    // ================= IWaveSound =================
    @Override
    public void onWaveStarted(int waveNumber) {
        play("wave_start.wav");
    }

    // ================= ITowerSound =================
    @Override
    public void onTowerPlaced(TowerType type) {
        play("tower_place.wav");
    }

    @Override
    public void onTowerUpgraded(Tower tower) {
        play("tower_upgrade.wav");
    }

    @Override
    public void onTowerSold(Tower tower) {
        play("tower_sell.wav");
    }

    // ================= IUnitSound =================
    @Override
    public void onUnitSpawned(LivingEntity unit) {
        // 出兵较频繁：音量压低（0.35 → 0.15），避免连续出兵刺耳
        playInternal("unit_spawn.wav", 1.0, 0.15);
    }

    // ================= ICombatSound =================
    @Override
    public void onProjectileFired(Projectile projectile) {
        // 追踪弹=箭矢出弦；定点弹(炮弹)发射静默，爆炸声在命中时播
        if (projectile != null && projectile.isTracking()) {
            playThrottled("arrow_release.wav", 60);
        }
    }

    @Override
    public void onProjectileHit(Projectile projectile, LivingEntity target) {
        // 追踪弹=箭矢命中；定点弹=炸弹爆炸（AOE）
        if (projectile != null && projectile.isTracking()) {
            playThrottled("arrow_hit.wav", 60);
        } else {
            playThrottled("bomb_hit.wav", 100);
        }
    }

    @Override
    public void onUnitAttack(LivingEntity attacker, LivingEntity target) {
        playThrottled("melee_attack.wav", 150);
    }

    @Override
    public void onUnitDied(LivingEntity unit) {
        // 按敌人类型区分死亡音色：Boss=巨兽低吼，坦克=低沉，普通/快速=常规
        double baseRate = 1.0;
        if (unit != null) {
            String type = unit.getClass().getSimpleName();
            if (type.contains("Boss")) baseRate = 0.65;
            else if (type.contains("Tank")) baseRate = 0.8;
        }
        playThrottledPitched("die_" + (random.nextInt(4) + 1) + ".wav", 150, baseRate);
    }

    @Override
    public void onBossStomp() {
        play("boss_stomp.wav");
    }

    // ================= IEndSound =================
    @Override
    public void onVictory() {
        play("win.wav");
    }

    @Override
    public void onGameOver() {
        play("lose.mp3");
    }

    // ================= 交互侧（D）扩展调用入口 =================
    /**
     * 漏怪扣生命提示音。
     * 建议调用点：IStatusObserver 生命值减少的回调处（交互侧接收状态推送时判断）。
     */
    public void onLifeLost() {
        play("life_lost.wav");
    }
}
