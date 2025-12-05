package com.project.DuAnTotNghiep.ghn;

import java.util.Map;

public interface GhnService {
    /**
     * Calculate fee from GHN for destination (districtId, wardCode) with the given
     * weight (grams)
     * Returns a map with keys e.g. fee (double), serviceName, estimatedDays
     */
    Map<String, Object> calculateFee(Integer districtId, String wardCode, Integer weight);

    // master data
    java.util.List<java.util.Map<String, Object>> getProvinces();

    java.util.List<java.util.Map<String, Object>> getDistricts(Integer provinceId);

    java.util.List<java.util.Map<String, Object>> getWards(Integer districtId);
}
