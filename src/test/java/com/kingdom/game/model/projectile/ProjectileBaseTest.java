package com.kingdom.game.model.projectile;

import com.kingdom.game.model.enemy.NormalEnemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Projectile base class: tracking flight, hit detection, damage.
 * Uses Arrow as concrete implementation (tracking mode).
 */
class ProjectileBaseTest {

    private Arrow arrow;
    private NormalEnemy target;

    @BeforeEach
    void setUp() {
        target = new NormalEnemy(200, 100, 80, 25, 15, 5, 1000);
        target.setPath(new double[]{300, 400}, new double[]{100, 100});
        arrow = new Arrow(0, 100, target, 10, 300);
    }

    @Test
    void initialPosition_correct() {
        assertEquals(0.0, arrow.getX());
        assertEquals(100.0, arrow.getY());
    }

    @Test
    void damageValue_correct() {
        assertEquals(10, arrow.getDamage());
    }

    @Test
    void trackingMode_isTracking() {
        assertTrue(arrow.isTracking());
    }

    @Test
    void trackingTarget_correct() {
        assertSame(target, arrow.getTargetEnemy());
    }

    @Test
    void initiallyNotHit() {
        assertFalse(arrow.hasHit());
    }

    @Test
    void update_movesTowardsTarget() {
        double oldX = arrow.getX();
        arrow.update();
        assertTrue(arrow.getX() > oldX);
        assertEquals(100.0, arrow.getY(), 0.001);
    }

    @Test
    void tracking_refreshesTargetCoordinates() {
        target.setX(250);
        target.setY(150);
        arrow.update();
        assertTrue(arrow.getY() > 100.0 || arrow.getY() < 100.0);
    }

    @Test
    void continuousFlight_reachesTarget() {
        for (int i = 0; i < 50; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
    }

    @Test
    void onHit_positionEqualsTarget() {
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
        assertEquals(target.getX(), arrow.getX(), 0.001);
        assertEquals(target.getY(), arrow.getY(), 0.001);
    }

    @Test
    void afterHit_updateDoesNotMove() {
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
        double x = arrow.getX();
        double y = arrow.getY();
        arrow.update();
        assertEquals(x, arrow.getX());
        assertEquals(y, arrow.getY());
    }

    @Test
    void targetDead_fliesToLastPosition() {
        target.takeDamage(100);
        assertFalse(target.isAlive());
        double targetX = target.getX();
        double targetY = target.getY();
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
        assertEquals(targetX, arrow.getX(), 0.001);
        assertEquals(targetY, arrow.getY(), 0.001);
    }

    @Test
    void onHit_dealsDamageToTarget() {
        for (int i = 0; i < 100; i++) {
            arrow.update();
            if (arrow.hasHit()) break;
        }
        assertTrue(arrow.hasHit());
        int hpBefore = target.getCurrentHp();
        arrow.onHit(List.of(target));
        assertEquals(hpBefore - 10, target.getCurrentHp());
    }

    @Test
    void onHit_deadTarget_noDamage() {
        target.takeDamage(100);
        arrow.onHit(List.of(target));
        assertEquals(0, target.getCurrentHp());
    }
}
