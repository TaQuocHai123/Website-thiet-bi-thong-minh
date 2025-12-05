package com.project.DuAnTotNghiep.service;

public interface GeocodeService {
    /**
     * Get estimated lat/lng for a district+ward combination.
     * Returns a map with keys: latitude, longitude
     */
    java.util.Map<String, Double> getCoordinatesForDistrictWard(Integer districtId, String wardCode);
}
