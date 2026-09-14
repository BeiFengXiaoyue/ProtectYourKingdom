package com.kingdom.game.util.map;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LevelWaves: verify maps/<key>/waves.json loads correctly.
 */
class LevelWavesTest {

    private static final String DEFAULT_WAVES = "/maps/default/waves.json";
    private static final String ROUND2_WAVES = "/maps/round2/waves.json";

    private LevelWaves loadWaves(String mapKey) {
        return LevelWaves.loadFromClasspath("/maps/" + mapKey + "/waves.json");
    }

    @Test
    void vocabulary_contains4EnemyTypes() {
        assertEquals(4, LevelWaves.VOCABULARY.size());
        assertTrue(LevelWaves.VOCABULARY.containsKey("normal_enemy"));
        assertTrue(LevelWaves.VOCABULARY.containsKey("fast_enemy"));
        assertTrue(LevelWaves.VOCABULARY.containsKey("tank_enemy"));
        assertTrue(LevelWaves.VOCABULARY.containsKey("boss_enemy"));
    }

    @Test
    void labelOf_knownType_returnsChineseName() {
        assertEquals("普通敌人", LevelWaves.labelOf("normal_enemy"));
        assertEquals("快速敌人", LevelWaves.labelOf("fast_enemy"));
        assertEquals("重甲敌人", LevelWaves.labelOf("tank_enemy"));
        assertEquals("首领敌人", LevelWaves.labelOf("boss_enemy"));
    }

    @Test
    void labelOf_unknownType_returnsTypeItself() {
        assertEquals("unknown_type", LevelWaves.labelOf("unknown_type"));
    }

    @Test
    void defaultMap_loadsSuccessfully() {
        LevelWaves waves = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        assertNotNull(waves);
    }

    @Test
    void defaultMap_has9Waves() {
        LevelWaves waves = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        assertNotNull(waves);
        assertEquals(9, waves.getWaves().size());
    }

    @Test
    void defaultMap_wave1_has5NormalEnemies() {
        LevelWaves waves = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        LevelWaves.Wave wave1 = waves.getWaves().get(0);
        assertEquals(1, wave1.getGroups().size());
        assertEquals("normal_enemy", wave1.getGroups().get(0).getType());
        assertEquals(5, wave1.getGroups().get(0).getCount());
    }

    @Test
    void defaultMap_wave9_has2Bosses() {
        LevelWaves waves = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        LevelWaves.Wave wave9 = waves.getWaves().get(8);
        long bossCount = wave9.getGroups().stream()
                .filter(g -> "boss_enemy".equals(g.getType()))
                .mapToInt(LevelWaves.SpawnGroup::getCount)
                .sum();
        assertEquals(2, bossCount);
    }

    @Test
    void defaultMap_wave9_containsTankAndFast() {
        LevelWaves waves = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        LevelWaves.Wave wave9 = waves.getWaves().get(8);
        boolean hasTank = wave9.getGroups().stream().anyMatch(g -> "tank_enemy".equals(g.getType()));
        boolean hasFast = wave9.getGroups().stream().anyMatch(g -> "fast_enemy".equals(g.getType()));
        assertTrue(hasTank);
        assertTrue(hasFast);
    }

    @Test
    void round2Map_loadsSuccessfully() {
        LevelWaves waves = LevelWaves.loadFromClasspath(ROUND2_WAVES);
        assertNotNull(waves);
    }

    @Test
    void round2Map_has9Waves() {
        LevelWaves waves = LevelWaves.loadFromClasspath(ROUND2_WAVES);
        assertNotNull(waves);
        assertEquals(9, waves.getWaves().size());
    }

    @Test
    void round2Map_wave1_has6NormalEnemies() {
        LevelWaves waves = LevelWaves.loadFromClasspath(ROUND2_WAVES);
        LevelWaves.Wave wave1 = waves.getWaves().get(0);
        assertEquals(6, wave1.getGroups().get(0).getCount());
    }

    @Test
    void round2Map_wave9_has3Bosses() {
        LevelWaves waves = LevelWaves.loadFromClasspath(ROUND2_WAVES);
        LevelWaves.Wave wave9 = waves.getWaves().get(8);
        long bossCount = wave9.getGroups().stream()
                .filter(g -> "boss_enemy".equals(g.getType()))
                .mapToInt(LevelWaves.SpawnGroup::getCount)
                .sum();
        assertEquals(3, bossCount);
    }

    @Test
    void round2Map_harderThanDefault_moreEnemies() {
        LevelWaves def = LevelWaves.loadFromClasspath(DEFAULT_WAVES);
        LevelWaves r2 = LevelWaves.loadFromClasspath(ROUND2_WAVES);
        int defTotal = def.getWaves().stream()
                .flatMap(w -> w.getGroups().stream())
                .mapToInt(LevelWaves.SpawnGroup::getCount).sum();
        int r2Total = r2.getWaves().stream()
                .flatMap(w -> w.getGroups().stream())
                .mapToInt(LevelWaves.SpawnGroup::getCount).sum();
        assertTrue(r2Total > defTotal);
    }

    @Test
    void nonexistentMap_returnsNull() {
        LevelWaves waves = LevelWaves.loadFromClasspath("/maps/nonexistent/waves.json");
        assertNull(waves);
    }

    @Test
    void allMaps_eachWave_hasAtLeastOneGroup() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            for (int i = 0; i < waves.getWaves().size(); i++) {
                assertFalse(waves.getWaves().get(i).getGroups().isEmpty(),
                        mapKey + " wave " + (i + 1) + " has no spawn groups");
            }
        }
    }

    @Test
    void allMaps_allEnemyTypes_inVocabulary() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            for (LevelWaves.Wave wave : waves.getWaves()) {
                for (LevelWaves.SpawnGroup group : wave.getGroups()) {
                    assertTrue(LevelWaves.VOCABULARY.containsKey(group.getType()),
                            mapKey + " unknown enemy type: " + group.getType());
                }
            }
        }
    }

    @Test
    void allMaps_allSpawnCounts_greaterThanZero() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            for (LevelWaves.Wave wave : waves.getWaves()) {
                for (LevelWaves.SpawnGroup group : wave.getGroups()) {
                    assertTrue(group.getCount() > 0,
                            mapKey + " wave " + wave.getId() + " " + group.getType() + " count=" + group.getCount());
                }
            }
        }
    }

    @Test
    void allMaps_allSpawnIntervals_greaterThanZero() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            for (LevelWaves.Wave wave : waves.getWaves()) {
                for (LevelWaves.SpawnGroup group : wave.getGroups()) {
                    assertTrue(group.getIntervalMs() > 0,
                            mapKey + " wave " + wave.getId() + " " + group.getType() + " interval=" + group.getIntervalMs());
                }
            }
        }
    }

    @Test
    void allMaps_have9Waves() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            assertNotNull(waves, mapKey + " failed to load");
            assertEquals(9, waves.getWaves().size(), mapKey + " should have 9 waves");
        }
    }

    @Test
    void allMaps_wave9_hasBoss() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            LevelWaves.Wave lastWave = waves.getWaves().get(8);
            boolean hasBoss = lastWave.getGroups().stream()
                    .anyMatch(g -> "boss_enemy".equals(g.getType()));
            assertTrue(hasBoss, mapKey + " wave 9 should have Boss");
        }
    }

    @Test
    void allMaps_wave1_startsWithNormalEnemy() {
        for (String mapKey : List.of("default", "round2", "round3", "round4", "round5")) {
            LevelWaves waves = loadWaves(mapKey);
            LevelWaves.Wave firstWave = waves.getWaves().get(0);
            assertEquals("normal_enemy", firstWave.getGroups().get(0).getType(),
                    mapKey + " wave 1 first group should be normal enemy");
        }
    }
}
