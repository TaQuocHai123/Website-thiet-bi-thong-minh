package com.project.DuAnTotNghiep.service.serviceImpl;

import com.project.DuAnTotNghiep.service.GeocodeService;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides estimated coordinates for Vietnamese districts and wards.
 * Used when user provides only district+ward codes (no explicit coordinates).
 */
@Service
public class GeocodeServiceImpl implements GeocodeService {

    @Override
    public Map<String, Double> getCoordinatesForDistrictWard(Integer districtId, String wardCode) {
        Map<String, Double> coords = new HashMap<>();

        // Hà Nội districts
        if (districtId == 1) { // Ba Đình
            coords.put("latitude", 21.0285);
            coords.put("longitude", 105.8142);
        } else if (districtId == 2) { // Hoàn Kiếm
            coords.put("latitude", 21.0285);
            coords.put("longitude", 105.8487);
        } else if (districtId == 3) { // Đống Đa
            coords.put("latitude", 21.0129);
            coords.put("longitude", 105.8292);
        } else if (districtId == 4) { // Hai Bà Trưng
            coords.put("latitude", 20.9984);
            coords.put("longitude", 105.8452);
        } else if (districtId == 5) { // Hoàng Mai
            coords.put("latitude", 20.9719);
            coords.put("longitude", 105.8737);
        } else if (districtId == 6) { // Long Biên
            coords.put("latitude", 21.0502);
            coords.put("longitude", 105.8840);
        } else if (districtId == 7) { // Tây Hồ
            coords.put("latitude", 21.0868);
            coords.put("longitude", 105.8168);
        } else if (districtId == 8) { // Cầu Giấy
            coords.put("latitude", 21.0285);
            coords.put("longitude", 105.7879);
        } else if (districtId == 9) { // Thanh Xuân
            coords.put("latitude", 20.9862);
            coords.put("longitude", 105.7977);
        } else if (districtId == 10) { // Hà Đông
            coords.put("latitude", 20.9445);
            coords.put("longitude", 105.7732);
        }
        // TP HCM districts
        else if (districtId == 101) { // Quận 1
            coords.put("latitude", 10.7624);
            coords.put("longitude", 106.6837);
        } else if (districtId == 102) { // Quận 3
            coords.put("latitude", 10.7967);
            coords.put("longitude", 106.6726);
        } else if (districtId == 103) { // Quận 5
            coords.put("latitude", 10.7599);
            coords.put("longitude", 106.6572);
        } else if (districtId == 104) { // Quận 10
            coords.put("latitude", 10.7579);
            coords.put("longitude", 106.6658);
        } else if (districtId == 105) { // Quận 11
            coords.put("latitude", 10.7372);
            coords.put("longitude", 106.6563);
        } else if (districtId == 106) { // Quận 12
            coords.put("latitude", 10.8765);
            coords.put("longitude", 106.7435);
        } else if (districtId == 107) { // Quận 4
            coords.put("latitude", 10.7515);
            coords.put("longitude", 106.7082);
        } else if (districtId == 108) { // Quận 6
            coords.put("latitude", 10.7281);
            coords.put("longitude", 106.6427);
        } else if (districtId == 109) { // Quận 7
            coords.put("latitude", 10.7359);
            coords.put("longitude", 106.7192);
        } else if (districtId == 110) { // Quận 8
            coords.put("latitude", 10.7209);
            coords.put("longitude", 106.7071);
        }
        // Hải Phòng
        else if (districtId == 201) { // Hồng Bàng
            coords.put("latitude", 20.8476);
            coords.put("longitude", 106.6860);
        }
        // Đà Nẵng
        else if (districtId == 301) { // Hải Châu
            coords.put("latitude", 16.0715);
            coords.put("longitude", 107.5899);
        }
        // Cần Thơ
        else if (districtId == 401) { // Ninh Kiều
            coords.put("latitude", 10.0379);
            coords.put("longitude", 105.7843);
        } else {
            // Default: return Hà Nội center
            coords.put("latitude", 21.0285);
            coords.put("longitude", 105.8407);
        }

        return coords;
    }

    /**
     * Static helper to get coordinates without needing service injection.
     */
    public static Map<String, Double> lookupCoordinates(Integer districtId, String wardCode) {
        GeocodeServiceImpl impl = new GeocodeServiceImpl();
        return impl.getCoordinatesForDistrictWard(districtId, wardCode);
    }
}
