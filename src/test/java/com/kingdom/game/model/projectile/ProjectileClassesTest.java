package com.kingdom.game.model.projectile;

import com.kingdom.game.model.enemy.NormalEnemy;
import com.kingdom.game.model.enemy.TankEnemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for projectile classes: Arrow (tracking single target) vs Bomb (fixed-point AOE armor-piercing).
 */
class ProjectileClassesTest {

    private NormalEnemy target;

    @BeforeEach
    void setUp() {
        target = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        target.setPath(new double[]{300, 400}, new double[]{100, 100});
    }

    // ================= Arrow =================

    @Test
    void arrow_isTrackingMode() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        assertTrue(arrow.isTracking());
        assertSame(target, arrow.getTargetEnemy());
    }

    @Test
    void arrow_initiallyNotHit() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        assertFalse(arrow.hasHit());
    }

    @Test
    void arrow_damageValueCorrect() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        assertEquals(10, arrow.getDamage());
    }

    @Test
    void arrow_onHit_dealsDamage_appliesReduction() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
        arrow.onHit(List.of(target));
        assertEquals(70, target.getCurrentHp());
    }

    @Test
    void arrow_vsTankEnemy_appliesReduction() {
        TankEnemy tank = new TankEnemy(200, 100, 240, 18, 35, 9, 1400);
        tank.setPath(new double[]{300, 400}, new double[]{100, 100});
        Arrow arrow = new Arrow(0, 100, tank, 10, 300);
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        arrow.onHit(List.of(tank));
        assertEquals(233, tank.getCurrentHp());
    }

    // ================= Bomb =================

    @Test
    void bomb_isFixedPointMode_notTracking() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertFalse(bomb.isTracking());
        assertNull(bomb.getTargetEnemy());
    }

    @Test
    void bomb_initiallyNotHit() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertFalse(bomb.hasHit());
    }

    @Test
    void bomb_damageValueCorrect() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertEquals(30, bomb.getDamage());
    }

    @Test
    void bomb_splashRadiusCorrect() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertEquals(50.0, bomb.getSplashRadius());
    }

    @Test
    void bomb_reachesTarget_thenHits() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        for (int i = 0; i < 100; i++) {
            bomb.update();
            if (bomb.hasHit()) break;
        }
        assertTrue(bomb.hasHit());
        assertEquals(200.0, bomb.getX(), 0.001);
        assertEquals(100.0, bomb.getY(), 0.001);
    }

    @Test
    void bomb_onHit_AOE_damagesAllEnemiesInRadius() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 100);
        NormalEnemy e1 = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        NormalEnemy e2 = new NormalEnemy(250, 100, 80, 25, 15, 5, 1000);
        for (int i = 0; i < 100; i++) {
            bomb.update();
            if (bomb.hasHit()) break;
        }
        bomb.onHit(List.of(e1, e2));
        assertEquals(50, e1.getCurrentHp());
        assertEquals(50, e2.getCurrentHp());
    }

    @Test
    void bomb_onHit_enemiesOutsideRadius_notDamaged() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        NormalEnemy near = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        NormalEnemy far = new NormalEnemy(500, 500, 80, 25, 15, 5, 1000);
        for (int i = 0; i < 100; i++) {
            bomb.update();
            if (bomb.hasHit()) break;
        }
        bomb.onHit(List.of(near, far));
        assertEquals(50, near.getCurrentHp());
        assertEquals(80, far.getCurrentHp());
    }

    @Test
    void bomb_damageIsArmorPiercing_ignoresTankReduction() {
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 100);
        TankEnemy tank = new TankEnemy(200, 100, 240, 18, 35, 9, 1400);
        for (int i = 0; i < 100; i++) {
            bomb.update();
            if (bomb.hasHit()) break;
        }
        bomb.onHit(List.of(tank));
        assertEquals(210, tank.getCurrentHp());
    }

    // ================= Arrow vs Bomb Comparison =================

    @Test
    void arrow_tracksTarget_bomb_fixedPoint() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertTrue(arrow.isTracking());
        assertFalse(bomb.isTracking());
    }

    @Test
    void bombDamage_higherThanArrow_sameTier() {
        Arrow arrow = new Arrow(0, 100, target, 10, 300);
        Bomb bomb = new Bomb(0, 100, 200, 100, 30, 200, 50);
        assertTrue(bomb.getDamage() > arrow.getDamage());
    }
}
