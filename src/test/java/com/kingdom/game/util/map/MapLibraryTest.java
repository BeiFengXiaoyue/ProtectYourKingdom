package com.kingdom.game.util.map;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapLibrary: map registry, classpath loading, entry lookup.
 */
class MapLibraryTest {

    @Test
    void listMapsFromClasspath_returnsAll5Maps() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        assertFalse(maps.isEmpty());
        assertTrue(maps.size() >= 5);
    }

    @Test
    void listMapsFromClasspath_containsDefault() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        boolean hasDefault = maps.stream().anyMatch(m -> "default".equals(m.getKey()));
        assertTrue(hasDefault);
    }

    @Test
    void listMapsFromClasspath_containsRound2ThroughRound5() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        for (String key : new String[]{"round2", "round3", "round4", "round5"}) {
            boolean found = maps.stream().anyMatch(m -> key.equals(m.getKey()));
            assertTrue(found, key + " should be in map registry");
        }
    }

    @Test
    void mapEntry_getKey_returnsKey() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        MapLibrary.MapEntry first = maps.get(0);
        assertNotNull(first.getKey());
        assertFalse(first.getKey().isBlank());
    }

    @Test
    void mapEntry_getName_notBlank() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        for (MapLibrary.MapEntry entry : maps) {
            assertNotNull(entry.getName());
            assertFalse(entry.getName().isBlank(), entry.getKey() + " should have a name");
        }
    }

    @Test
    void mapEntry_getImage_notBlank() {
        List<MapLibrary.MapEntry> maps = MapLibrary.listMapsFromClasspath();
        for (MapLibrary.MapEntry entry : maps) {
            assertNotNull(entry.getImage());
            assertFalse(entry.getImage().isBlank(), entry.getKey() + " should have an image");
        }
    }

    @Test
    void defaultKey_returnsFirstMapKey() {
        String key = MapLibrary.defaultKey();
        assertNotNull(key);
        assertFalse(key.isBlank());
    }

    @Test
    void exists_default_returnsTrue() {
        assertTrue(MapLibrary.exists("default"));
    }

    @Test
    void exists_nonexistent_returnsFalse() {
        assertFalse(MapLibrary.exists("nonexistent_map_xyz"));
    }

    @Test
    void exists_null_returnsFalse() {
        assertFalse(MapLibrary.exists(null));
    }

    @Test
    void exists_blank_returnsFalse() {
        assertFalse(MapLibrary.exists(""));
    }

    @Test
    void getEntry_default_returnsEntry() {
        MapLibrary.MapEntry entry = MapLibrary.getEntry("default");
        assertNotNull(entry);
        assertEquals("default", entry.getKey());
    }

    @Test
    void getEntry_nonexistent_returnsNull() {
        assertNull(MapLibrary.getEntry("nonexistent_map_xyz"));
    }

    @Test
    void readPathFromClasspath_default_loads() {
        MapRoute route = MapLibrary.readPathFromClasspath("default");
        assertNotNull(route);
        assertTrue(route.pointCount() >= 2);
    }

    @Test
    void readSpotsFromClasspath_default_loads() {
        TowerSpots spots = MapLibrary.readSpotsFromClasspath("default");
        assertNotNull(spots);
        assertTrue(spots.spotCount() > 0);
    }

    @Test
    void readWavesFromClasspath_default_loads() {
        LevelWaves waves = MapLibrary.readWavesFromClasspath("default");
        assertNotNull(waves);
        assertEquals(9, waves.getWaves().size());
    }

    @Test
    void readWavesFromClasspath_nonexistent_returnsNull() {
        assertNull(MapLibrary.readWavesFromClasspath("nonexistent"));
    }

    @Test
    void entryLabel_containsKeyAndName() {
        MapLibrary.MapEntry entry = MapLibrary.getEntry("default");
        String label = MapLibrary.entryLabel(entry);
        assertTrue(label.contains("default"));
        assertTrue(label.contains(entry.getName()));
    }

    @Test
    void entryLabel_null_returnsEmpty() {
        assertEquals("", MapLibrary.entryLabel(null));
    }

    @Test
    void allMaps_havePathSpotsAndWaves() {
        for (String key : new String[]{"default", "round2", "round3", "round4", "round5"}) {
            assertNotNull(MapLibrary.readPathFromClasspath(key), key + " path");
            assertNotNull(MapLibrary.readSpotsFromClasspath(key), key + " spots");
            assertNotNull(MapLibrary.readWavesFromClasspath(key), key + " waves");
        }
    }
}
