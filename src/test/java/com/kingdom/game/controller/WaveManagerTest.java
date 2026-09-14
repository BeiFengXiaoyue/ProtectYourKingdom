package com.kingdom.game.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaveManager: spawn queue, timeline, pause, reset.
 */
class WaveManagerTest {

    private WaveManager waveManager;
    private List<String> spawnedTypes;

    @BeforeEach
    void setUp() {
        waveManager = new WaveManager();
        spawnedTypes = new ArrayList<>();
    }

    @Test
    void initiallyNotSpawning() {
        assertFalse(waveManager.isSpawning());
    }

    @Test
    void beginWave_startsSpawning() {
        waveManager.beginWave(5, 1000);
        assertTrue(waveManager.isSpawning());
    }

    @Test
    void firstEnemy_spawnsImmediately() {
        waveManager.beginWave(3, 1000);
        long now = System.nanoTime();
        waveManager.update(now, spawnedTypes::add);
        assertEquals(1, spawnedTypes.size());
    }

    @Test
    void secondEnemy_notBeforeInterval() {
        waveManager.beginWave(2, 5000);
        long now = System.nanoTime();
        waveManager.update(now + 1000_000_000L, spawnedTypes::add);
        assertEquals(1, spawnedTypes.size());
        assertTrue(waveManager.isSpawning());
    }

    @Test
    void secondEnemy_spawnsAfterInterval() {
        waveManager.beginWave(2, 1000);
        long now = System.nanoTime();
        waveManager.update(now, spawnedTypes::add);
        assertEquals(1, spawnedTypes.size());
        waveManager.update(now + 2000_000_000L, spawnedTypes::add);
        assertEquals(2, spawnedTypes.size());
    }

    @Test
    void allEnemiesSpawned_spawningEnds() {
        waveManager.beginWave(3, 100);
        long now = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            waveManager.update(now + i * 200_000_000L, spawnedTypes::add);
        }
        assertEquals(3, spawnedTypes.size());
        assertFalse(waveManager.isSpawning());
    }

    @Test
    void reset_clearsSpawnQueue() {
        waveManager.beginWave(5, 1000);
        waveManager.reset();
        assertFalse(waveManager.isSpawning());
    }

    @Test
    void reset_afterPartialSpawn() {
        waveManager.beginWave(5, 1000);
        long now = System.nanoTime();
        waveManager.update(now, spawnedTypes::add);
        assertEquals(1, spawnedTypes.size());
        waveManager.reset();
        assertFalse(waveManager.isSpawning());
    }

    @Test
    void beginWave_withZeroEnemies() {
        waveManager.beginWave(0, 1000);
        assertFalse(waveManager.isSpawning());
    }

    @Test
    void multipleBeginWaves_lastOneWins() {
        waveManager.beginWave(5, 1000);
        waveManager.beginWave(2, 500);
        long now = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            waveManager.update(now + i * 600_000_000L, spawnedTypes::add);
        }
        assertEquals(2, spawnedTypes.size());
    }
}
