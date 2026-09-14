package com.kingdom.game.util.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TowerSpots: construction, JSON round-trip, classpath loading, validation.
 */
class TowerSpotsTest {

    private static final double[] XS = {150, 420, 600};
    private static final double[] YS = {120, 90, 200};

    @Test
    void of_withName_setsAllFields() {
        TowerSpots spots = TowerSpots.of("my_spots", 700, 600, XS, YS);
        assertEquals("my_spots", spots.getName());
        assertEquals(700.0, spots.getWidth());
        assertEquals(600.0, spots.getHeight());
        assertEquals(3, spots.spotCount());
    }

    @Test
    void of_simpleConstructor_usesDefaultName() {
        TowerSpots spots = TowerSpots.of(700, 600, XS, YS);
        assertEquals("tower_spots", spots.getName());
    }

    @Test
    void getXs_returnsCopy() {
        TowerSpots spots = TowerSpots.of(700, 600, XS, YS);
        double[] xs = spots.getXs();
        xs[0] = 999;
        assertEquals(150.0, spots.getXs()[0]);
    }

    @Test
    void getYs_returnsCopy() {
        TowerSpots spots = TowerSpots.of(700, 600, XS, YS);
        double[] ys = spots.getYs();
        ys[0] = 999;
        assertEquals(120.0, spots.getYs()[0]);
    }

    @Test
    void toJson_containsNameAndDimensions() {
        TowerSpots spots = TowerSpots.of("test", 800, 500, XS, YS);
        String json = spots.toJson();
        assertTrue(json.contains("\"name\": \"test\""));
        assertTrue(json.contains("\"width\": 800"));
        assertTrue(json.contains("\"height\": 500"));
    }

    @Test
    void toJson_containsAllSpots() {
        TowerSpots spots = TowerSpots.of(700, 600, XS, YS);
        String json = spots.toJson();
        assertTrue(json.contains("\"x\":150,\"y\":120"));
        assertTrue(json.contains("\"x\":420,\"y\":90"));
        assertTrue(json.contains("\"x\":600,\"y\":200"));
    }

    @Test
    void fromJson_parsesCorrectly() {
        String json = "{\"name\":\"s\",\"width\":700,\"height\":600,\"spots\":[{\"x\":10,\"y\":20},{\"x\":30,\"y\":40}]}";
        TowerSpots spots = TowerSpots.fromJson(json);
        assertEquals("s", spots.getName());
        assertEquals(700.0, spots.getWidth());
        assertEquals(600.0, spots.getHeight());
        assertEquals(2, spots.spotCount());
    }

    @Test
    void jsonRoundTrip_preservesData() {
        TowerSpots original = TowerSpots.of("roundtrip", 800, 600, XS, YS);
        TowerSpots parsed = TowerSpots.fromJson(original.toJson());
        assertEquals(original.getName(), parsed.getName());
        assertEquals(original.getWidth(), parsed.getWidth());
        assertEquals(original.getHeight(), parsed.getHeight());
        assertEquals(original.spotCount(), parsed.spotCount());
        assertArrayEquals(original.getXs(), parsed.getXs());
        assertArrayEquals(original.getYs(), parsed.getYs());
    }

    @Test
    void fromJson_missingSpots_throws() {
        String json = "{\"name\":\"s\",\"width\":700,\"height\":600}";
        assertThrows(IllegalArgumentException.class, () -> TowerSpots.fromJson(json));
    }

    @Test
    void fromJson_emptySpots_allowed() {
        String json = "{\"name\":\"s\",\"width\":700,\"height\":600,\"spots\":[]}";
        TowerSpots spots = TowerSpots.fromJson(json);
        assertEquals(0, spots.spotCount());
    }

    @Test
    void of_zeroWidth_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> TowerSpots.of(0, 600, XS, YS));
    }

    @Test
    void of_mismatchedArrayLengths_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> TowerSpots.of(700, 600, new double[]{1, 2}, new double[]{1}));
    }

    @Test
    void loadFromClasspath_defaultMap_loads() {
        TowerSpots spots = TowerSpots.loadFromClasspath("/maps/default/spots.json");
        assertNotNull(spots);
        assertTrue(spots.spotCount() > 0);
    }

    @Test
    void loadFromClasspath_nonexistent_returnsNull() {
        assertNull(TowerSpots.loadFromClasspath("/maps/nonexistent/spots.json"));
    }

    @Test
    void allMaps_haveValidSpots() {
        for (String key : new String[]{"default", "round2", "round3", "round4", "round5"}) {
            TowerSpots spots = TowerSpots.loadFromClasspath("/maps/" + key + "/spots.json");
            assertNotNull(spots, key + " spots.json should exist");
            assertTrue(spots.spotCount() > 0, key + " should have at least 1 tower spot");
        }
    }
}
