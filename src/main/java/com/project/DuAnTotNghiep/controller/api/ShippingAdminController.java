package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.service.serviceImpl.MasterDataCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class ShippingAdminController {

    private final MasterDataCacheService cacheService;

    public ShippingAdminController(MasterDataCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @PostMapping("/api/admin/shipping/import-ghn")
    public ResponseEntity<Map<String, Object>> importGhnMaster() {
        boolean ok = cacheService.importMasterData();
        return ResponseEntity.ok(Map.of("ok", ok));
    }

    @GetMapping("/api/shipping/cache/provinces")
    public ResponseEntity<List<Map<String, Object>>> getCachedProvinces() {
        return ResponseEntity.ok(cacheService.getProvincesFromCache());
    }

    @GetMapping("/api/shipping/cache/districts")
    public ResponseEntity<List<Map<String, Object>>> getCachedDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(cacheService.getDistrictsFromCache(provinceId));
    }

    @GetMapping("/api/shipping/cache/wards")
    public ResponseEntity<List<Map<String, Object>>> getCachedWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(cacheService.getWardsFromCache(districtId));
    }
}
