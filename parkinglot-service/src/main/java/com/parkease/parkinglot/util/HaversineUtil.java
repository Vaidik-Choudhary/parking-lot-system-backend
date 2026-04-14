package com.parkease.parkinglot.util;


public class HaversineUtil {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private HaversineUtil() {}

    public static double calculateDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(radLat1) * Math.cos(radLat2)
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }
}
