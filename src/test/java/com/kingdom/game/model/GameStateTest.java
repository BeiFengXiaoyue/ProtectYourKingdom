package com.kingdom.game.model;

import com.kingdom.game.config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GameState: gold, lives, wave, reset, boundary values.
 */
class GameStateTest {

    private GameState state;
    private int initialGold;

    @BeforeEach
    void setUp() {
        state = new GameState(new GameConfig());
        initialGold = state.getGold();
    }

    @Test
    void initialGold_isPositive() {
        assertTrue(initialGold > 0);
    }

    @Test
    void initialLives_is20() {
        assertEquals(20, state.getLives());
    }

    @Test
    void initialWave_is0() {
        assertEquals(0, state.getWave());
    }

    @Test
    void addGold_increasesGold() {
        state.addGold(50);
        assertEquals(initialGold + 50, state.getGold());
    }

    @Test
    void spendGold_decreasesGold() {
        state.spendGold(30);
        assertEquals(initialGold - 30, state.getGold());
    }

    @Test
    void spendGold_returnsTrue_whenSufficient() {
        assertTrue(state.spendGold(initialGold / 2));
    }

    @Test
    void spendGold_returnsFalse_whenInsufficient() {
        assertFalse(state.spendGold(initialGold + 100));
    }

    @Test
    void spendGold_insufficient_goldUnchanged() {
        state.spendGold(initialGold + 100);
        assertEquals(initialGold, state.getGold());
    }

    @Test
    void loseLives_decreasesLives() {
        state.loseLives(1);
        assertEquals(19, state.getLives());
    }

    @Test
    void loseLives_multiple() {
        state.loseLives(3);
        assertEquals(17, state.getLives());
    }

    @Test
    void loseLives_doesNotGoNegative() {
        state.loseLives(100);
        assertEquals(0, state.getLives());
    }

    @Test
    void isGameOver_true_whenLivesZero() {
        state.loseLives(20);
        assertTrue(state.isGameOver());
    }

    @Test
    void isGameOver_false_whenLivesPositive() {
        assertFalse(state.isGameOver());
    }

    @Test
    void advanceWave_increasesWaveNumber() {
        state.advanceWave();
        assertEquals(1, state.getWave());
    }

    @Test
    void advanceWave_multipleTimes() {
        state.advanceWave();
        state.advanceWave();
        state.advanceWave();
        assertEquals(3, state.getWave());
    }

    @Test
    void reset_restoresInitialValues() {
        state.addGold(50);
        state.loseLives(1);
        state.advanceWave();
        state.reset();
        assertEquals(initialGold, state.getGold());
        assertEquals(20, state.getLives());
        assertEquals(0, state.getWave());
        assertFalse(state.isGameOver());
    }

    @Test
    void addGold_zeroAmount_noChange() {
        state.addGold(0);
        assertEquals(initialGold, state.getGold());
    }

    @Test
    void spendGold_zeroAmount_returnsTrue() {
        assertTrue(state.spendGold(0));
        assertEquals(initialGold, state.getGold());
    }

    @Test
    void totalWaves_isPositive() {
        assertTrue(state.getTotalWaves() > 0);
    }
}
