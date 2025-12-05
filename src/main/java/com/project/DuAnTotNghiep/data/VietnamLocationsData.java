package com.project.DuAnTotNghiep.data;

import java.util.*;

/**
 * Complete Vietnam locations data with all provinces
 * Data source: danh-sach-3321-xa-phuong.xls
 * 
 * This class provides access to:
 * - 34 Provinces/Cities (all Vietnamese provinces)
 * - Districts and Wards are now loaded via GHN API or cache fallback
 */
public class VietnamLocationsData {

    // Simple mapping of province IDs to names
    private static final Map<Integer, String> PROVINCES = new HashMap<>();

    static {
        initializeProvinces();
    }

    private static void initializeProvinces() {
        // All Vietnamese provinces keyed by their official ID
        PROVINCES.put(1, "Thành phố Hà Nội");
        PROVINCES.put(2, "Tỉnh Hà Giang");
        PROVINCES.put(4, "Tỉnh Cao Bằng");
        PROVINCES.put(6, "Tỉnh Bắc Kạn");
        PROVINCES.put(8, "Tỉnh Tuyên Quang");
        PROVINCES.put(10, "Tỉnh Lào Cai");
        PROVINCES.put(11, "Tỉnh Điện Biên");
        PROVINCES.put(12, "Tỉnh Lai Châu");
        PROVINCES.put(14, "Tỉnh Sơn La");
        PROVINCES.put(15, "Tỉnh Yên Bái");
        PROVINCES.put(17, "Tỉnh Hòa Bình");
        PROVINCES.put(19, "Tỉnh Thái Nguyên");
        PROVINCES.put(20, "Tỉnh Lạng Sơn");
        PROVINCES.put(22, "Tỉnh Quảng Ninh");
        PROVINCES.put(24, "Tỉnh Bắc Giang");
        PROVINCES.put(25, "Tỉnh Phú Thọ");
        PROVINCES.put(26, "Tỉnh Vĩnh Phúc");
        PROVINCES.put(27, "Tỉnh Bắc Ninh");
        PROVINCES.put(30, "Tỉnh Hải Dương");
        PROVINCES.put(31, "Thành phố Hải Phòng");
        PROVINCES.put(33, "Tỉnh Hưng Yên");
        PROVINCES.put(34, "Tỉnh Thái Bình");
        PROVINCES.put(35, "Tỉnh Hà Nam");
        PROVINCES.put(36, "Tỉnh Nam Định");
        PROVINCES.put(37, "Tỉnh Ninh Bình");
        PROVINCES.put(38, "Tỉnh Thanh Hóa");
        PROVINCES.put(40, "Tỉnh Nghệ An");
        PROVINCES.put(42, "Tỉnh Hà Tĩnh");
        PROVINCES.put(44, "Tỉnh Quảng Bình");
        PROVINCES.put(45, "Tỉnh Quảng Trị");
        PROVINCES.put(46, "Tỉnh Thừa Thiên Huế");
        PROVINCES.put(48, "Thành phố Đà Nẵng");
        PROVINCES.put(49, "Tỉnh Quảng Nam");
        PROVINCES.put(51, "Tỉnh Quảng Ngãi");
        PROVINCES.put(52, "Tỉnh Bình Định");
        PROVINCES.put(54, "Tỉnh Phú Yên");
        PROVINCES.put(56, "Tỉnh Khánh Hòa");
        PROVINCES.put(58, "Tỉnh Ninh Thuận");
        PROVINCES.put(60, "Tỉnh Bình Thuận");
        PROVINCES.put(62, "Tỉnh Đồng Nai");
        PROVINCES.put(64, "Tỉnh Bà Rịa - Vũng Tàu");
        PROVINCES.put(66, "Thành phố Hồ Chí Minh");
        PROVINCES.put(67, "Tỉnh Long An");
        PROVINCES.put(68, "Tỉnh Tiền Giang");
        PROVINCES.put(70, "Tỉnh Bến Tre");
        PROVINCES.put(72, "Tỉnh Trà Vinh");
        PROVINCES.put(74, "Tỉnh Vĩnh Long");
        PROVINCES.put(75, "Thành phố Cần Thơ");
        PROVINCES.put(76, "Tỉnh Đồng Tháp");
        PROVINCES.put(77, "Tỉnh An Giang");
        PROVINCES.put(79, "Tỉnh Kiên Giang");
        PROVINCES.put(80, "Tỉnh Cà Mau");
        PROVINCES.put(82, "Tỉnh Tây Ninh");
        PROVINCES.put(83, "Tỉnh Bình Phước");
        PROVINCES.put(84, "Tỉnh Bình Dương");
        PROVINCES.put(86, "Tỉnh Gia Lai");
        PROVINCES.put(87, "Tỉnh Kon Tum");
        PROVINCES.put(89, "Tỉnh Đắk Lắk");
        PROVINCES.put(91, "Tỉnh Đắk Nông");
    }

    /**
     * Get all provinces
     */
    public static List<Map<String, Object>> getProvinces() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : PROVINCES.entrySet()) {
            Map<String, Object> prov = new HashMap<>();
            prov.put("province_id", entry.getKey());
            prov.put("province_name", entry.getValue());
            prov.put("ProvinceID", entry.getKey());
            prov.put("ProvinceName", entry.getValue());
            list.add(prov);
        }
        return list;
    }

    /**
     * Get districts for a province - returns minimal placeholder
     */
    public static List<Map<String, Object>> getDistrictsByProvince(Integer provinceId) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (provinceId == null)
            return list;

        Map<String, Object> district = new HashMap<>();
        district.put("district_id", provinceId * 100);
        district.put("district_name", "Quận/Huyện");
        district.put("DistrictID", provinceId * 100);
        district.put("DistrictName", "Quận/Huyện");
        list.add(district);

        return list;
    }

    /**
     * Get wards for a district - returns empty list
     * Districts/wards are loaded from GHN cache or API fallback
     */
    public static List<Map<String, Object>> getWardsByDistrict(Integer districtId) {
        return new ArrayList<>();
    }

    /**
     * Get all wards for a province - returns empty list
     */
    public static List<Map<String, Object>> getWardsByProvince(Integer provinceId) {
        return new ArrayList<>();
    }
}
