package com.project.DuAnTotNghiep.controller.api;

import com.project.DuAnTotNghiep.service.ShippingService;
import com.project.DuAnTotNghiep.ghn.GhnService;
import com.project.DuAnTotNghiep.data.VietnamLocationsData;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

@RestController
public class ShippingController {
    private final ShippingService shippingService;
    private final GhnService ghnService;
    private final com.project.DuAnTotNghiep.service.serviceImpl.MasterDataCacheService masterDataCacheService;
    private final RestTemplate restTemplate = new RestTemplate();

    public ShippingController(ShippingService shippingService, GhnService ghnService,
            com.project.DuAnTotNghiep.service.serviceImpl.MasterDataCacheService masterDataCacheService) {
        this.shippingService = shippingService;
        this.ghnService = ghnService;
        this.masterDataCacheService = masterDataCacheService;
    }

    @GetMapping("/api/shipping/fee")
    public ResponseEntity<Map<String, Object>> getShippingFee(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Integer districtId,
            @RequestParam(required = false) String wardCode,
            @RequestParam(required = false) Integer weight) {
        Map<String, Object> res = new HashMap<>();

        // If districtId is 0 or null but wardCode is provided, try to resolve districtId from cache
        Integer resolvedDistrictId = districtId;
        if ((resolvedDistrictId == null || resolvedDistrictId == 0) && wardCode != null) {
            System.out.println("Attempting to resolve districtId from wardCode: " + wardCode);
            List<java.util.Map<String, Object>> wards = masterDataCacheService.getAllWardsFromCache();
            if (wards != null && !wards.isEmpty()) {
                for (java.util.Map<String, Object> w : wards) {
                    String wcode = extractString(w, "WardCode", "Code", "code", "ward_code");
                    if (wardCode.equals(wcode)) {
                        resolvedDistrictId = extractInt(w, "DistrictID", "district_id", "DistrictId");
                        System.out.println("Resolved wardCode " + wardCode + " to districtId: " + resolvedDistrictId);
                        break;
                    }
                }
            }
        }

        // Try GHN API if district+ward provided (after resolution)
        if (resolvedDistrictId != null && resolvedDistrictId > 0 && wardCode != null) {
            try {
                Map<String, Object> ghnResult = ghnService.calculateFee(resolvedDistrictId, wardCode,
                        weight == null ? 1000 : weight);
                if (ghnResult != null && ghnResult.get("fee") != null) {
                    Double fee = ((Number) ghnResult.get("fee")).doubleValue();
                    if (fee > 0) {
                        res.put("fee", fee);
                        res.put("provider", ghnResult.get("provider"));
                        res.put("raw", ghnResult.get("raw"));
                        return ResponseEntity.ok(res);
                    }
                }
            } catch (Exception e) {
                System.err.println("GHN fee calculation failed: " + e.getMessage());
            }

            // GHN failed → use local zone-based pricing by districtId
            Double fee = ((com.project.DuAnTotNghiep.service.serviceImpl.ShippingServiceImpl) shippingService)
                    .calculateShippingFeeByDistrict(resolvedDistrictId);
            res.put("fee", fee);
            res.put("provider", "LOCAL_ZONE");
            System.out.println("Using LOCAL_ZONE pricing for districtId: " + resolvedDistrictId + " → fee: " + fee);
            return ResponseEntity.ok(res);
        }

        // If lat/lng provided, use coordinate-based pricing
        if (lat != null && lng != null) {
            Double fee = shippingService.calculateShippingFee(lat, lng);
            res.put("fee", fee);
            res.put("provider", "LOCAL_COORDS");
            return ResponseEntity.ok(res);
        }

        res.put("fee", 0);
        res.put("provider", "UNKNOWN");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/api/shipping/ghn/provinces")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getGhnProvinces() {
        // prefer cached master data when available
        java.util.List<java.util.Map<String, Object>> cached = masterDataCacheService.getProvincesFromCache();
        if (cached != null && !cached.isEmpty())
            return ResponseEntity.ok(cached);
        return ResponseEntity.ok(ghnService.getProvinces());
    }

    @GetMapping("/api/shipping/ghn/districts")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getGhnDistricts(
            @RequestParam Integer provinceId) {
        java.util.List<java.util.Map<String, Object>> cached = masterDataCacheService.getDistrictsFromCache(provinceId);
        if (cached != null && !cached.isEmpty())
            return ResponseEntity.ok(cached);
        return ResponseEntity.ok(ghnService.getDistricts(provinceId));
    }

    @GetMapping("/api/shipping/ghn/wards")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getGhnWards(@RequestParam Integer districtId) {
        java.util.List<java.util.Map<String, Object>> cached = masterDataCacheService.getWardsFromCache(districtId);
        if (cached != null && !cached.isEmpty())
            return ResponseEntity.ok(cached);
        return ResponseEntity.ok(ghnService.getWards(districtId));
    }

    private List<Map<String, Object>> getHardcodedProvinces() {
        List<Map<String, Object>> provinces = new ArrayList<>();
        String[][] data = {
                { "1", "Hà Nội" }, { "2", "Hà Giang" }, { "4", "Cao Bằng" }, { "6", "Bắc Kạn" },
                { "8", "Tuyên Quang" }, { "10", "Lào Cai" }, { "11", "Điện Biên" }, { "12", "Lai Châu" },
                { "14", "Sơn La" }, { "15", "Yên Bái" }, { "17", "Hòa Bình" }, { "19", "Thái Nguyên" },
                { "20", "Lạng Sơn" }, { "22", "Quảng Ninh" }, { "24", "Bắc Giang" }, { "25", "Phú Thọ" },
                { "26", "Vĩnh Phúc" }, { "27", "Bắc Ninh" }, { "30", "Hải Dương" }, { "31", "Hải Phòng" },
                { "33", "Hưng Yên" }, { "34", "Thái Bình" }, { "35", "Hà Nam" }, { "36", "Nam Định" },
                { "37", "Ninh Bình" }, { "38", "Thanh Hóa" }, { "40", "Nghệ An" }, { "42", "Hà Tĩnh" },
                { "44", "Quảng Bình" }, { "45", "Quảng Trị" }, { "46", "Thừa Thiên Huế" }, { "48", "Đà Nẵng" },
                { "49", "Quảng Nam" }, { "51", "Quảng Ngãi" }, { "52", "Bình Định" }, { "54", "Phú Yên" },
                { "56", "Khánh Hòa" }, { "58", "Ninh Thuận" }, { "60", "Bình Thuận" }, { "62", "Đồng Nai" },
                { "64", "Bà Rịa - Vũng Tàu" }, { "66", "TP Hồ Chí Minh" }, { "67", "Long An" },
                { "68", "Tiền Giang" }, { "70", "Bến Tre" }, { "72", "Trà Vinh" }, { "74", "Vĩnh Long" },
                { "75", "Cần Thơ" }, { "76", "Đồng Tháp" }, { "77", "An Giang" }, { "79", "Kiên Giang" },
                { "80", "Cà Mau" }, { "82", "Tây Ninh" }, { "83", "Bình Phước" }, { "84", "Bình Dương" },
                { "86", "Gia Lai" }, { "87", "Kon Tum" }, { "89", "Đắk Lắk" }, { "91", "Đắk Nông" }
        };
        for (String[] p : data) {
            Map<String, Object> prov = new HashMap<>();
            prov.put("province_id", Integer.parseInt(p[0]));
            prov.put("province_name", p[1]);
            prov.put("ProvinceID", Integer.parseInt(p[0]));
            prov.put("ProvinceName", p[1]);
            provinces.add(prov);
        }
        return provinces;
    }

    @GetMapping("/api/shipping/vapi/provinces")
    public List<Map<String, Object>> getVapiProvinces() {
        try {
            String url = "https://vapi.vnappmob.com/api/province/";
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getBody() != null && resp.getBody().containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.getBody().get("results");
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            }
        } catch (Exception e) {
            System.err.println("Vapi provinces fallback failed: " + e.getMessage());
        }
        // Return hardcoded Vietnamese provinces as final fallback
        return getHardcodedProvinces();
    }

    @GetMapping("/api/shipping/vapi/districts")
    public List<Map<String, Object>> getVapiDistricts(@RequestParam Integer provinceId) {
        // VAPI districts endpoint: try VAPI first, then static fallback (do NOT mix
        // with GHN IDs)
        try {
            String url = "https://vapi.vnappmob.com/api/province/district/" + provinceId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getBody() != null && resp.getBody().containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.getBody().get("results");
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            }
        } catch (Exception e) {
            System.err.println("Vapi districts fallback failed for province " + provinceId + ": " + e.getMessage());
        }

        // Use static Vietnam locations data as fallback (matching VAPI province ID)
        List<Map<String, Object>> staticData = VietnamLocationsData.getDistrictsByProvince(provinceId);
        if (staticData != null && !staticData.isEmpty()) {
            return staticData;
        }

        // Final fallback: empty list
        return new ArrayList<>();
    }

    @GetMapping("/api/shipping/vapi/wards")
    public List<Map<String, Object>> getVapiWards(@RequestParam Integer districtId) {
        // VAPI wards endpoint: try VAPI first, then static fallback (do NOT mix with
        // GHN IDs)
        try {
            String url = "https://vapi.vnappmob.com/api/province/ward/" + districtId;
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getBody() != null && resp.getBody().containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) resp.getBody().get("results");
                if (results != null && !results.isEmpty()) {
                    return results;
                }
            }
        } catch (Exception e) {
            System.err.println("Vapi wards fallback failed for district " + districtId + ": " + e.getMessage());
        }

        // Use static Vietnam locations data as fallback (matching VAPI district ID)
        List<Map<String, Object>> staticData = VietnamLocationsData.getWardsByDistrict(districtId);
        if (staticData != null && !staticData.isEmpty()) {
            return staticData;
        }

        // Minimal static fallback for some districts (backward compat)
        if (districtId != null) {
            if (districtId == 1) {
                List<Map<String, Object>> staticW = new ArrayList<>();
                staticW.add(makeWard(1001, "Phường Trúc Bạch"));
                staticW.add(makeWard(1002, "Phường Trần Hưng Đạo"));
                return staticW;
            }
            if (districtId == 101) {
                List<Map<String, Object>> staticW = new ArrayList<>();
                staticW.add(makeWard(2001, "Phường Bến Nghé"));
                staticW.add(makeWard(2002, "Phường Tân Định"));
                return staticW;
            }
        }

        return new ArrayList<>();
    }

    private Integer extractInt(java.util.Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object o = map.get(k);
                if (o instanceof Number)
                    return ((Number) o).intValue();
                if (o instanceof String) {
                    try {
                        return Integer.parseInt((String) o);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return null;
    }

    private String extractString(java.util.Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                Object o = map.get(k);
                if (o != null)
                    return String.valueOf(o);
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null)
            return "";
        try {
            String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
            n = n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
            n = n.replaceAll("[^\\u0000-\\u007F]+", "");
            n = n.replaceAll("[\\p{Punct}]", " ");
            n = n.replaceAll("\\s+", " ").trim().toLowerCase();
            return n;
        } catch (Exception e) {
            return s == null ? "" : s.toLowerCase();
        }
    }

    private Map<String, Object> makeWard(int id, String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("ward_id", id);
        m.put("ward_name", name);
        m.put("WardID", id);
        m.put("WardName", name);
        return m;
    }

    /**
     * New endpoint: return generic districts for any province
     * This returns placeholder districts that work with the frontend
     */
    @GetMapping("/api/shipping/districts-list")
    public List<Map<String, Object>> getDistrictsList(@RequestParam Integer provinceId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (provinceId == null || provinceId <= 0)
            return list;

        // Try to resolve the province name from our static VAPI-like list
        String vapiProvName = null;
        try {
            List<Map<String, Object>> provs = VietnamLocationsData.getProvinces();
            for (Map<String, Object> p : provs) {
                Integer pid = extractInt(p, "province_id", "ProvinceID");
                if (pid != null && pid.equals(provinceId)) {
                    vapiProvName = extractString(p, "province_name", "ProvinceName");
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        // Load GHN provinces (cached or live)
        List<java.util.Map<String, Object>> ghnProvs = masterDataCacheService.getProvincesFromCache();
        if (ghnProvs == null || ghnProvs.isEmpty()) {
            ghnProvs = ghnService.getProvinces();
        }

        Integer matchedGhnProvinceId = null;
        if (vapiProvName != null && ghnProvs != null) {
            String vapiClean = normalize(vapiProvName);
            for (java.util.Map<String, Object> gp : ghnProvs) {
                String ghnName = extractString(gp, "ProvinceName", "province_name", "name");
                Integer ghpId = extractInt(gp, "ProvinceID", "province_id");
                if (ghnName == null || ghpId == null)
                    continue;
                String ghnClean = normalize(ghnName);
                if (vapiClean.contains(ghnClean) || ghnClean.contains(vapiClean) || ghnClean.startsWith(vapiClean)
                        || vapiClean.startsWith(ghnClean)) {
                    matchedGhnProvinceId = ghpId;
                    break;
                }
            }
        }

        // If matched GHN province found, fetch districts from cache or GHN
        if (matchedGhnProvinceId != null) {
            List<java.util.Map<String, Object>> districtsData = masterDataCacheService
                    .getDistrictsFromCache(matchedGhnProvinceId);
            if (districtsData == null || districtsData.isEmpty()) {
                districtsData = ghnService.getDistricts(matchedGhnProvinceId);
            }
            if (districtsData != null && !districtsData.isEmpty()) {
                for (java.util.Map<String, Object> d : districtsData) {
                    Integer did = extractInt(d, "DistrictID", "district_id", "DistrictId", "district_id");
                    String dname = extractString(d, "DistrictName", "district_name", "name");
                    if (did == null)
                        continue;
                    Map<String, Object> item = new HashMap<>();
                    item.put("district_id", did);
                    item.put("district_name", dname == null ? "" : dname);
                    item.put("DistrictID", did);
                    item.put("DistrictName", dname == null ? "" : dname);
                    list.add(item);
                }
                return list;
            }
        }

        // Fallback: use static wards file grouping to synthesize districts if no GHN
        // data
        List<Map<String, Object>> staticDistricts = VietnamLocationsData.getDistrictsByProvince(provinceId);
        if (staticDistricts != null && !staticDistricts.isEmpty())
            return staticDistricts;

        // Final fallback: return empty list
        return list;
    }

    /**
     * New endpoint: return generic wards for any district
     * This returns placeholder wards that work with the frontend
     */
    @GetMapping("/api/shipping/wards-list")
    public List<Map<String, Object>> getWardsList(@RequestParam Integer districtId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (districtId == null || districtId <= 0)
            return list;

        // Try cache first
        List<java.util.Map<String, Object>> wardsData = masterDataCacheService.getWardsFromCache(districtId);
        if (wardsData == null || wardsData.isEmpty()) {
            wardsData = ghnService.getWards(districtId);
        }
        if (wardsData != null && !wardsData.isEmpty()) {
            for (java.util.Map<String, Object> w : wardsData) {
                // GHN wards use WardCode as the identifier (string), not WardID
                String wcode = extractString(w, "WardCode", "Code", "code", "ward_code");
                String wname = extractString(w, "WardName", "ward_name", "name");
                if (wcode == null)
                    continue;
                // Generate a numeric ID from the ward code if needed
                Integer wid = null;
                try {
                    wid = Integer.parseInt(wcode);
                } catch (Exception ignored) {
                    wid = wcode.hashCode();
                }
                Map<String, Object> item = new HashMap<>();
                item.put("ward_id", wid);
                item.put("ward_code", wcode);
                item.put("ward_name", wname == null ? "" : wname);
                item.put("WardID", wid);
                item.put("WardCode", wcode);
                item.put("WardName", wname == null ? "" : wname);
                list.add(item);
            }
            return list;
        }

        // Fallback: use static wards grouped by district (if available in
        // VietnamLocationsData)
        List<Map<String, Object>> staticWards = VietnamLocationsData.getWardsByDistrict(districtId);
        if (staticWards != null && !staticWards.isEmpty())
            return staticWards;

        return list;
    }
}
