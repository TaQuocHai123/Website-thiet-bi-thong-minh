package com.project.DuAnTotNghiep.service.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.DuAnTotNghiep.ghn.GhnService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class MasterDataCacheService {

    private final GhnService ghnService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final File cacheFile = new File("data/ghn-master.json");

    public MasterDataCacheService(GhnService ghnService) {
        this.ghnService = ghnService;
    }

    public boolean importMasterData() {
        try {
            List<Map<String, Object>> provinces = ghnService.getProvinces();
            Map<String, Object> out = new HashMap<>();
            out.put("provinces", provinces == null ? Collections.emptyList() : provinces);

            List<Map<String, Object>> districtsAll = new ArrayList<>();
            List<Map<String, Object>> wardsAll = new ArrayList<>();

            if (provinces != null) {
                for (Map<String, Object> p : provinces) {
                    Integer provinceId = null;
                    Object pid = p.get("ProvinceID");
                    if (pid == null)
                        pid = p.get("province_id");
                    if (pid instanceof Number)
                        provinceId = ((Number) pid).intValue();
                    else if (pid instanceof String) {
                        try {
                            provinceId = Integer.parseInt((String) pid);
                        } catch (Exception ignored) {
                        }
                    }
                    if (provinceId == null)
                        continue;
                    List<Map<String, Object>> districts = ghnService.getDistricts(provinceId);
                    if (districts != null) {
                        for (Map<String, Object> d : districts) {
                            districtsAll.add(d);
                            Integer districtId = null;
                            Object did = d.get("DistrictID");
                            if (did == null)
                                did = d.get("district_id");
                            if (did instanceof Number)
                                districtId = ((Number) did).intValue();
                            else if (did instanceof String) {
                                try {
                                    districtId = Integer.parseInt((String) did);
                                } catch (Exception ignored) {
                                }
                            }
                            if (districtId == null)
                                continue;
                            List<Map<String, Object>> wards = ghnService.getWards(districtId);
                            if (wards != null) {
                                for (Map<String, Object> w : wards) {
                                    wardsAll.add(w);
                                }
                            }
                        }
                    }
                }
            }

            out.put("districts", districtsAll);
            out.put("wards", wardsAll);

            // ensure parent dir
            File parent = cacheFile.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, out);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> loadCache() {
        try {
            if (!cacheFile.exists())
                return Collections.emptyMap();
            return mapper.readValue(cacheFile, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getProvincesFromCache() {
        Map<String, Object> m = loadCache();
        Object o = m.get("provinces");
        if (o instanceof List)
            return (List<Map<String, Object>>) o;
        return Collections.emptyList();
    }

    public List<Map<String, Object>> getDistrictsFromCache(Integer provinceId) {
        Map<String, Object> m = loadCache();
        Object o = m.get("districts");
        if (!(o instanceof List))
            return Collections.emptyList();
        List<Map<String, Object>> all = (List<Map<String, Object>>) o;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> d : all) {
            Object pid = d.get("ProvinceID");
            if (pid == null)
                pid = d.get("province_id");
            Integer p = null;
            if (pid instanceof Number)
                p = ((Number) pid).intValue();
            else if (pid instanceof String) {
                try {
                    p = Integer.parseInt((String) pid);
                } catch (Exception ex) {
                }
            }
            if (p != null && p.equals(provinceId))
                out.add(d);
        }
        return out;
    }

    public List<Map<String, Object>> getWardsFromCache(Integer districtId) {
        Map<String, Object> m = loadCache();
        Object o = m.get("wards");
        if (!(o instanceof List))
            return Collections.emptyList();
        List<Map<String, Object>> all = (List<Map<String, Object>>) o;
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> w : all) {
            Object did = w.get("DistrictID");
            if (did == null)
                did = w.get("district_id");
            Integer dId = null;
            if (did instanceof Number)
                dId = ((Number) did).intValue();
            else if (did instanceof String) {
                try {
                    dId = Integer.parseInt((String) did);
                } catch (Exception ex) {
                }
            }
            if (dId != null && dId.equals(districtId))
                out.add(w);
        }
        return out;
    }

    public List<Map<String, Object>> getAllWardsFromCache() {
        Map<String, Object> m = loadCache();
        Object o = m.get("wards");
        if (o instanceof List)
            return (List<Map<String, Object>>) o;
        return Collections.emptyList();
    }
}
