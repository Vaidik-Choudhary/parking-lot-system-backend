package com.parkease.parkinglot.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HaversineUtilTest {
    @Test void samePoint_shouldBeZero() {
        assertEquals(0.0, HaversineUtil.calculateDistance(19.0, 72.8, 19.0, 72.8));
    }
    @Test void mumbaiPune_shouldBeAround150km() {
        double d = HaversineUtil.calculateDistance(19.0760, 72.8777, 18.5204, 73.8567);
        assertTrue(d > 100 && d < 200);
    }
    @Test void result_shouldBeRoundedTo2Decimals() {
        double d = HaversineUtil.calculateDistance(19.0, 72.8, 18.5, 73.8);
        assertEquals(d, Math.round(d * 100.0) / 100.0);
    }
    @Test void isSymmetric() {
        double d1 = HaversineUtil.calculateDistance(19.0, 72.8, 18.5, 73.8);
        double d2 = HaversineUtil.calculateDistance(18.5, 73.8, 19.0, 72.8);
        assertEquals(d1, d2);
    }
}
