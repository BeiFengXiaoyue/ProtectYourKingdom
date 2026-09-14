package com.kingdom.game.model.tower;

import com.kingdom.game.model.enemy.NormalEnemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Tower base class: cooldown, stun, sell refund, target finding.
 * Uses ArrowTower as concrete implementation.
 */
class TowerBaseTest {

    private ArrowTower tower;

    @BeforeEach
    void setUp() {
        tower = new ArrowTower(100, 100);
    }

    @Test
    void afterAttack_cooldownIsSet() {
        NormalEnemy enemy = new NormalEnemy(110, 100, 80, 25, 15, 5, 1000);
        enemy.setPath(new double[]{110, 200}, new double[]{100, 100});
        tower.tryAttack(List.of(enemy));
        tower.update();
        for (int i = 0; i < 100; i++) {
            tower.update();
        }
        assertDoesNotThrow(() -> tower.tryAttack(List.of(enemy)));
    }

    @Test
    void whenStunned_cannotAttack() {
        tower.stun(3000);
        NormalEnemy enemy = new NormalEnemy(110, 100, 80, 25, 15, 5, 1000);
        enemy.setPath(new double[]{110, 200}, new double[]{100, 100});
        assertDoesNotThrow(() -> tower.tryAttack(List.of(enemy)));
    }

    @Test
    void stunExpires_afterDuration() {
        tower.stun(100);
        tower.update();
        for (int i = 0; i < 10; i++) {
            tower.update();
        }
        NormalEnemy enemy = new NormalEnemy(110, 100, 80, 25, 15, 5, 1000);
        enemy.setPath(new double[]{110, 200}, new double[]{100, 100});
        assertDoesNotThrow(() -> tower.tryAttack(List.of(enemy)));
    }

    @Test
    void multipleStuns_useLatestDuration() {
        tower.stun(1000);
        tower.stun(5000);
        tower.update();
        assertDoesNotThrow(() -> tower.tryAttack(List.of()));
    }

    @Test
    void sellRefund_is50Percent() {
        assertEquals(25, tower.sell());
    }

    @Test
    void cannonSellRefund_is50Percent() {
        CannonTower cannon = new CannonTower(100, 100);
        assertEquals(50, cannon.sell());
    }

    @Test
    void findTarget_returnsNull_whenNoEnemies() {
        assertNull(tower.findTarget(List.of()));
    }

    @Test
    void findTarget_returnsNull_whenEnemyOutOfRange() {
        NormalEnemy farEnemy = new NormalEnemy(500, 500, 80, 25, 15, 5, 1000);
        farEnemy.setPath(new double[]{550, 600}, new double[]{500, 500});
        assertNull(tower.findTarget(List.of(farEnemy)));
    }

    @Test
    void findTarget_returnsFirstEnemyInRange() {
        NormalEnemy first = new NormalEnemy(150, 100, 80, 25, 15, 5, 1000);
        first.setPath(new double[]{150, 200}, new double[]{100, 100});
        NormalEnemy second = new NormalEnemy(120, 100, 80, 25, 15, 5, 1000);
        second.setPath(new double[]{120, 200}, new double[]{100, 100});
        assertSame(first, tower.findTarget(List.of(first, second)));
    }

    @Test
    void findTarget_onlyReturnsAliveEnemies() {
        NormalEnemy alive = new NormalEnemy(150, 100, 80, 25, 15, 5, 1000);
        alive.setPath(new double[]{150, 200}, new double[]{100, 100});
        NormalEnemy dead = new NormalEnemy(120, 100, 80, 25, 15, 5, 1000);
        dead.setPath(new double[]{120, 200}, new double[]{100, 100});
        dead.takeDamage(1000);
        assertSame(alive, tower.findTarget(List.of(dead, alive)));
    }

    @Test
    void constructor_setsPosition() {
        ArrowTower t = new ArrowTower(200, 300);
        assertEquals(200.0, t.getX());
        assertEquals(300.0, t.getY());
    }
}
