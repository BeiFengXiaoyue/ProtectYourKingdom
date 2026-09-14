package com.kingdom.game.model;

import com.kingdom.game.model.enemy.NormalEnemy;
import com.kingdom.game.model.enemy.TankEnemy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LivingEntity base class: damage, reduction, melee attack, stun, collision, faction.
 * Uses NormalEnemy as concrete implementation.
 */
class LivingEntityTest {

    private NormalEnemy enemy;
    private static final double[] PATH_X = {100, 200};
    private static final double[] PATH_Y = {0, 0};

    @BeforeEach
    void setUp() {
        enemy = new NormalEnemy(0, 0, 100, 25, 15, 10, 1000);
        enemy.setPath(PATH_X.clone(), PATH_Y.clone());
    }

    @Test
    void initialHp_is100() {
        assertEquals(100, enemy.getCurrentHp());
        assertEquals(100, enemy.getMaxHp());
    }

    @Test
    void initialSpeed_is25() {
        assertEquals(25.0, enemy.getSpeed());
    }

    @Test
    void initialFaction_isEnemy() {
        assertEquals(Faction.ENEMY, enemy.getFaction());
    }

    @Test
    void initiallyAlive() {
        assertTrue(enemy.isAlive());
    }

    @Test
    void initiallyNotStunned() {
        assertFalse(enemy.isStunned());
    }

    @Test
    void attackDamage_is10() {
        assertEquals(10, enemy.getAttackDamage());
    }

    @Test
    void maxAttackCooldown_is1000() {
        assertEquals(1000, enemy.getMaxAttackCooldown());
    }

    @Test
    void takeDamage_normal_reducesHp() {
        int actual = enemy.takeDamage(30);
        assertEquals(30, actual);
        assertEquals(70, enemy.getCurrentHp());
    }

    @Test
    void multipleTakeDamage_accumulates() {
        enemy.takeDamage(20);
        enemy.takeDamage(30);
        assertEquals(50, enemy.getCurrentHp());
    }

    @Test
    void takeDamage_returnsActualDamage() {
        int actual = enemy.takeDamage(25);
        assertEquals(25, actual);
    }

    @Test
    void hpZero_dies() {
        enemy.takeDamage(100);
        assertEquals(0, enemy.getCurrentHp());
        assertFalse(enemy.isAlive());
    }

    @Test
    void overkill_hpNotNegative() {
        enemy.takeDamage(999);
        assertEquals(0, enemy.getCurrentHp());
    }

    @Test
    void deadEnemy_takeDamage_returnsZero() {
        enemy.takeDamage(100);
        int actual = enemy.takeDamage(50);
        assertEquals(0, actual);
        assertEquals(0, enemy.getCurrentHp());
    }

    @Test
    void tankEnemy_hasPhysicalReduction() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35, 9, 1400);
        int actual = tank.takeDamage(10);
        assertEquals(7, actual);
        assertEquals(233, tank.getCurrentHp());
    }

    @Test
    void armorPiercing_ignoresReduction() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35, 9, 1400);
        int actual = tank.takeDamage(10, true);
        assertEquals(10, actual);
        assertEquals(230, tank.getCurrentHp());
    }

    @Test
    void smallDamage_hasMinimumOne() {
        TankEnemy tank = new TankEnemy(0, 0, 240, 18, 35, 9, 1400);
        int actual = tank.takeDamage(1);
        assertEquals(1, actual);
    }

    @Test
    void tryAttack_normal_dealsDamage() {
        NormalEnemy target = new NormalEnemy(50, 0, 100, 25, 15, 5, 1000);
        target.setPath(PATH_X.clone(), PATH_Y.clone());
        enemy.tryAttack(target);
        assertEquals(90, target.getCurrentHp());
    }

    @Test
    void tryAttack_setsCooldown() {
        NormalEnemy target = new NormalEnemy(50, 0, 100, 25, 15, 5, 1000);
        target.setPath(PATH_X.clone(), PATH_Y.clone());
        enemy.tryAttack(target);
        int hpBefore = target.getCurrentHp();
        enemy.tryAttack(target);
        assertEquals(hpBefore, target.getCurrentHp());
    }

    @Test
    void tryAttack_whenStunned_doesNothing() {
        NormalEnemy target = new NormalEnemy(50, 0, 100, 25, 15, 5, 1000);
        target.setPath(PATH_X.clone(), PATH_Y.clone());
        enemy.stun(3000);
        int hpBefore = target.getCurrentHp();
        enemy.tryAttack(target);
        assertEquals(hpBefore, target.getCurrentHp());
    }

    @Test
    void tryAttack_deadTarget_doesNothing() {
        NormalEnemy target = new NormalEnemy(50, 0, 100, 25, 15, 5, 1000);
        target.setPath(PATH_X.clone(), PATH_Y.clone());
        target.takeDamage(100);
        enemy.tryAttack(target);
        assertEquals(0, target.getCurrentHp());
    }

    @Test
    void tryAttack_nullTarget_doesNotThrow() {
        assertDoesNotThrow(() -> enemy.tryAttack(null));
    }

    @Test
    void stun_setsStunnedState() {
        enemy.stun(1000);
        assertTrue(enemy.isStunned());
    }

    @Test
    void update_stunTimerDecrements() {
        enemy.stun(100);
        enemy.update();
        assertTrue(enemy.isStunned());
        for (int i = 0; i < 10; i++) {
            enemy.update();
        }
        assertFalse(enemy.isStunned());
    }

    @Test
    void collisionEnabled_whenAliveAndNotStunned() {
        assertTrue(enemy.isCollisionEnabled());
    }

    @Test
    void collisionDisabled_whenDead() {
        enemy.takeDamage(100);
        assertFalse(enemy.isCollisionEnabled());
    }

    @Test
    void collisionDisabled_whenStunned() {
        enemy.stun(1000);
        assertFalse(enemy.isCollisionEnabled());
    }

    @Test
    void collisionRadius_isHalfOfMaxDimension() {
        double expected = Math.max(enemy.getWidth(), enemy.getHeight()) / 2.0;
        assertEquals(expected, enemy.getCollisionRadius());
    }

    @Test
    void setFaction_changesFaction() {
        enemy.setFaction(Faction.ALLY);
        assertEquals(Faction.ALLY, enemy.getFaction());
    }

    @Test
    void sameFactionCollision_doesNotTrigger() {
        NormalEnemy other = new NormalEnemy(50, 0, 100, 25, 15, 5, 1000);
        other.setPath(PATH_X.clone(), PATH_Y.clone());
        boolean result = enemy.onCollision(other);
        assertFalse(result);
    }
}
