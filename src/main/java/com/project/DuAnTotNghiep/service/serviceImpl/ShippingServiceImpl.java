package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.service.ShippingService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ShippingServiceImpl implements ShippingService {

    // Store coordinates (Ho Chi Minh City center - Gò Vấp)
    private static final double STORE_LAT = 10.8231;
    private static final double STORE_LNG = 106.6297;

    // Shipping fee table (no weight factor)
    private static final double FEE_NOI_TINH_HANOI_HCM = 15500.0; // Nội thành HN/HCM
    private static final double FEE_NOI_TINH_KHAC = 21000.0; // Nội tỉnh khác
    private static final double FEE_NOI_VUNG = 29000.0; // Nội vùng
    private static final double FEE_NOI_VUNG_TINH = 29000.0; // Nội vùng tính
    private static final double FEE_LIEN_VUNG_DBIET = 34000.0; // Liên vùng đặc biệt
    private static final double FEE_LIEN_VUNG = 39000.0; // Liên vùng
    private static final double FEE_LIEN_TINH = 36000.0; // Liên tỉnh

    // Map GHN districtId → delivery zone (must be initialized first)
    private static Map<Integer, String> districtZoneMap = new HashMap<>();

    static {
        initializeDistrictZoneMap();
    }

    private static void initializeDistrictZoneMap() {
        // HCM inner city districts (from GHN: 1442-1463, 3695)
        Integer[] hcmInner = { 1442, 1443, 1444, 1446, 1447, 1448, 1449, 1450, 1451, 1452, 1453, 1454, 1455, 1456, 
                1457, 1458, 1459, 1461, 1462, 1463, 3695 };
        for (Integer did : hcmInner) {
            districtZoneMap.put(did, "NOI_TINH_HCM");
        }

        // HCM outer + nearby provinces (from GHN: 1460, 1533, 1534, 2090)
        Integer[] hcmOuterAndNearby = { 1460, 1533, 1534, 2090 };
        for (Integer did : hcmOuterAndNearby) {
            districtZoneMap.put(did, "NOI_VUNG");
        }

        // Hanoi inner districts (add actual Hanoi districts if available in future)
        // For now, using coordinate-based determination
    }

    /**
     * Calculate shipping fee by districtId
     * Used when lat/lng not available - looks up zone by district
     */
    public Double calculateShippingFeeByDistrict(Integer districtId) {
        if (districtId == null || districtId <= 0) {
            return FEE_LIEN_TINH; // Default to inter-province
        }

        String zone = districtZoneMap.getOrDefault(districtId, "LIEN_TINH");
        return getRoundedFee(zone);
    }

    @Override
    public Double calculateShippingFee(Double latitude, Double longitude) {
        if (latitude == null || longitude == null)
            return 0.0;

        // Determine zone based on coordinates
        String zone = determineZoneByCoords(latitude, longitude);
        return getRoundedFee(zone);
    }

    /**
     * Determine delivery zone by coordinates (distance-based)
     */
    private String determineZoneByCoords(double lat, double lng) {
        // Check if within HCM city bounds
        if (isWithinHCM(lat, lng)) {
            return "NOI_TINH_HCM";
        }
        // Check if within Hanoi bounds
        if (isWithinHanoi(lat, lng)) {
            return "NOI_TINH_HANOI";
        }

        // Calculate distance from store to destination
        double distKm = haversineDistanceKm(STORE_LAT, STORE_LNG, lat, lng);
        if (distKm <= 100) {
            return "NOI_VUNG";
        } else if (distKm <= 300) {
            return "NOI_VUNG_TINH";
        } else if (distKm <= 500) {
            return "LIEN_VUNG_DBIET";
        } else if (distKm <= 800) {
            return "LIEN_VUNG";
        } else {
            return "LIEN_TINH";
        }
    }

    private double getRoundedFee(String zone) {
        double fee;
        switch (zone) {
            case "NOI_TINH_HCM":
            case "NOI_TINH_HANOI":
                fee = FEE_NOI_TINH_HANOI_HCM;
                break;
            case "NOI_TINH_KHAC":
                fee = FEE_NOI_TINH_KHAC;
                break;
            case "NOI_VUNG":
                fee = FEE_NOI_VUNG;
                break;
            case "NOI_VUNG_TINH":
                fee = FEE_NOI_VUNG_TINH;
                break;
            case "LIEN_VUNG_DBIET":
                fee = FEE_LIEN_VUNG_DBIET;
                break;
            case "LIEN_VUNG":
                fee = FEE_LIEN_VUNG;
                break;
            case "LIEN_TINH":
                fee = FEE_LIEN_TINH;
                break;
            default:
                fee = FEE_NOI_VUNG;
        }
        // Round to nearest 1000
        return Math.round(fee / 1000.0) * 1000.0;
    }

    /**
     * Check if coordinates are within HCM city bounds
     */
    private boolean isWithinHCM(double lat, double lng) {
        // HCM city bounds: 10.5°-11.0° N, 106.4°-107.0° E
        return lat >= 10.5 && lat <= 11.0 && lng >= 106.4 && lng <= 107.0;
    }

    /**
     * Check if coordinates are within Hanoi bounds
     */
    private boolean isWithinHanoi(double lat, double lng) {
        // Hanoi bounds: 20.8°-21.2° N, 105.7°-106.1° E
        return lat >= 20.8 && lat <= 21.2 && lng >= 105.7 && lng <= 106.1;
    }

    private static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // in kilometers
        return distance;
    }
}
