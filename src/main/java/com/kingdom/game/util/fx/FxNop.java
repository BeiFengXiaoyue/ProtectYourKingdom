package com.kingdom.game.util.fx;

import com.kingdom.game.controller.ICombatSound;
import com.kingdom.game.controller.IEndSound;
import com.kingdom.game.controller.IFloatingTextFx;
import com.kingdom.game.controller.IParticleFx;
import com.kingdom.game.controller.IScreenFx;
import com.kingdom.game.controller.ISelectionFx;
import com.kingdom.game.controller.ITowerSound;
import com.kingdom.game.controller.IUnitSound;
import com.kingdom.game.controller.IWaveSound;
import com.kingdom.game.model.GameObject;
import com.kingdom.game.model.LivingEntity;
import com.kingdom.game.model.TowerType;
import com.kingdom.game.model.projectile.Projectile;
import com.kingdom.game.model.tower.Tower;

/**
 * FxNop —— 各事件接口的"空实现单例"集合。
 *
 * 背景：接口契约坚持"无 default 方法、方法必须由实现方实现"。
 * 为让尚未注入任何实现（如音效管理器、特效层）的早期开发阶段不抛 NPE，
 * 统一以这里的空实现作为默认值；GameController 在实体注册/放置时注入真实实现替换。
 */
public final class FxNop {

    private FxNop() {
    }

    public static final IWaveSound NO_WAVE_SOUND = new IWaveSound() {
        @Override public void onWaveStarted(int waveNumber) { }
    };

    public static final ITowerSound NO_TOWER_SOUND = new ITowerSound() {
        @Override public void onTowerPlaced(TowerType type) { }
        @Override public void onTowerUpgraded(Tower tower) { }
        @Override public void onTowerSold(Tower tower) { }
    };

    public static final IUnitSound NO_UNIT_SOUND = new IUnitSound() {
        @Override public void onUnitSpawned(LivingEntity unit) { }
    };

    public static final ICombatSound NO_COMBAT_SOUND = new ICombatSound() {
        @Override public void onProjectileFired(Projectile projectile) { }
        @Override public void onProjectileHit(Projectile projectile, LivingEntity target) { }
        @Override public void onUnitAttack(LivingEntity attacker, LivingEntity target) { }
        @Override public void onUnitDied(LivingEntity unit) { }
        @Override public void onBossStomp() { }
    };

    public static final IEndSound NO_END_SOUND = new IEndSound() {
        @Override public void onVictory() { }
        @Override public void onGameOver() { }
    };

    public static final IFloatingTextFx NO_FLOATING_TEXT = new IFloatingTextFx() {
        @Override public void showFloatingText(double x, double y, String text, String color) { }
    };

    public static final IScreenFx NO_SCREEN_FX = new IScreenFx() {
        @Override public void flashScreen(String color, int durationMs) { }
        @Override public void shakeScreen(int durationMs, int intensity) { }
        @Override public void showBossWarning() { }
    };

    public static final IParticleFx NO_PARTICLE_FX = new IParticleFx() {
        @Override public void spawnExplosionParticles(double x, double y, String color, int count) { }
    };

    public static final ISelectionFx NO_SELECTION_FX = new ISelectionFx() {
        @Override public void showSelectionHighlight(GameObject target) { }
        @Override public void clearSelectionHighlight() { }
    };
}
