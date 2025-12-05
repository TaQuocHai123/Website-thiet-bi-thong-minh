package com.project.DuAnTotNghiep.service;

public interface ShippingService {
    /**
     * Calculate shipping fee based on coordinates (in VNĐ)
     */
    Double calculateShippingFee(Double latitude, Double longitude);
}
