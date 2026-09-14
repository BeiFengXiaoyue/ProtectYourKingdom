package com.kingdom.game.util.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapRoute: construction, JSON round-trip, classpath loading, validation.
 */
class MapRouteTest {

    private static final double[] XS = {60, 300, 300, 600};
    private static final double[] YS = {160, 160, 400, 400};

    @Test
    void of_withNameAndImage_setsAllFields() {
        MapRoute route = MapRoute.of("test_path", "map.png", 900, 560, XS, YS);
        assertEquals("test_path", route.getName());
        assertEquals("map.png", route.getImage());
        assertEquals(900.0, route.getWidth());
        assertEquals(560.0, route.getHeight());
        assertEquals(4, route.pointCount());
    }

    @Test
    void of_simpleConstructor_usesDefaults() {
        MapRoute route = MapRoute.of(900, 560, XS, YS);
        assertEquals("default_path", route.getName());
        assertNull(route.getImage());
    }

    @Test
    void getXs_returnsCopy() {
        MapRoute route = MapRoute.of(900, 560, XS, YS);
        double[] xs = route.getXs();
        xs[0] = 999;
        assertEquals(60.0, route.getXs()[0]);
    }

    @Test
    void getYs_returnsCopy() {
        MapRoute route = MapRoute.of(900, 560, XS, YS);
        double[] ys = route.getYs();
        ys[0] = 999;
        assertEquals(160.0, route.getYs()[0]);
    }

    @Test
    void toJson_containsNameAndDimensions() {
        MapRoute route = MapRoute.of("my_path", "bg.png", 800, 600, XS, YS);
        String json = route.toJson();
        assertTrue(json.contains("\"name\": \"my_path\""));
        assertTrue(json.contains("\"image\": \"bg.png\""));
        assertTrue(json.contains("\"width\": 800"));
        assertTrue(json.contains("\"height\": 600"));
    }

    @Test
    void toJson_containsAllPoints() {
        MapRoute route = MapRoute.of(900, 560, XS, YS);
        String json = route.toJson();
        assertTrue(json.contains("\"x\":60,\"y\":160"));
        assertTrue(json.contains("\"x\":300,\"y\":160"));
        assertTrue(json.contains("\"x\":300,\"y\":400"));
        assertTrue(json.contains("\"x\":600,\"y\":400"));
    }

    @Test
    void fromJson_parsesCorrectly() {
        String json = "{\"name\":\"p\",\"width\":900,\"height\":560,\"points\":[{\"x\":10,\"y\":20},{\"x\":30,\"y\":40}]}";
        MapRoute route = MapRoute.fromJson(json);
        assertEquals("p", route.getName());
        assertEquals(900.0, route.getWidth());
        assertEquals(560.0, route.getHeight());
        assertEquals(2, route.pointCount());
        assertEquals(10.0, route.getXs()[0]);
        assertEquals(20.0, route.getYs()[0]);
    }

    @Test
    void jsonRoundTrip_preservesData() {
        MapRoute original = MapRoute.of("roundtrip", "bg.png", 800, 600, XS, YS);
        MapRoute parsed = MapRoute.fromJson(original.toJson());
        assertEquals(original.getName(), parsed.getName());
        assertEquals(original.getImage(), parsed.getImage());
        assertEquals(original.getWidth(), parsed.getWidth());
        assertEquals(original.getHeight(), parsed.getHeight());
        assertEquals(original.pointCount(), parsed.pointCount());
        assertArrayEquals(original.getXs(), parsed.getXs());
        assertArrayEquals(original.getYs(), parsed.getYs());
    }

    @Test
    void fromJson_missingPoints_throws() {
        String json = "{\"name\":\"p\",\"width\":900,\"height\":560}";
        assertThrows(IllegalArgumentException.class, () -> MapRoute.fromJson(json));
    }

    @Test
    void fromJson_singlePoint_throws() {
        String json = "{\"name\":\"p\",\"width\":900,\"height\":560,\"points\":[{\"x\":10,\"y\":20}]}";
        assertThrows(IllegalArgumentException.class, () -> MapRoute.fromJson(json));
    }

    @Test
    void fromJson_zeroWidth_throws() {
        String json = "{\"name\":\"p\",\"width\":0,\"height\":560,\"points\":[{\"x\":10,\"y\":20},{\"x\":30,\"y\":40}]}";
        assertThrows(IllegalArgumentException.class, () -> MapRoute.fromJson(json));
    }

    @Test
    void of_mismatchedArrayLengths_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MapRoute.of(900, 560, new double[]{1, 2}, new double[]{1}));
    }

    @Test
    void of_nullXs_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MapRoute.of(900, 560, null, YS));
    }

    @Test
    void loadFromClasspath_defaultMap_loads() {
        MapRoute route = MapRoute.loadFromClasspath("/maps/default/path.json");
        assertNotNull(route);
        assertTrue(route.pointCount() >= 2);
        assertTrue(route.getWidth() > 0);
        assertTrue(route.getHeight() > 0);
    }

    @Test
    void loadFromClasspath_nonexistent_returnsNull() {
        assertNull(MapRoute.loadFromClasspath("/maps/nonexistent/path.json"));
    }

    @Test
    void allMaps_haveValidPaths() {
        for (String key : new String[]{"default", "round2", "round3", "round4", "round5"}) {
            MapRoute route = MapRoute.loadFromClasspath("/maps/" + key + "/path.json");
            assertNotNull(route, key + " path.json should exist");
            assertTrue(route.pointCount() >= 2, key + " should have at least 2 path points");
        }
    }
}
