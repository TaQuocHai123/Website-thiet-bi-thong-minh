package com.project.DuAnTotNghiep.ghn;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GhnServiceImpl implements GhnService {

    private final GhnConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public GhnServiceImpl(GhnConfig ghnConfig) {
        this.ghnConfig = ghnConfig;
    }

    @Override
    public Map<String, Object> calculateFee(Integer districtId, String wardCode, Integer weight) {
        Map<String, Object> result = new HashMap<>();
        try {
            // If no GHN token configured, return null and let caller fallback
            if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty()) {
                result.put("fee", 0);
                result.put("provider", "GHN");
                result.put("error", "token not configured");
                return result;
            }

            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/v2/shipping-order/fee";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            headers.set("ShopId", ghnConfig.getShopId());

            // Minimal body for fee calc; insurance_value is 0; we pass GHN service id from
            // config
            Map<String, Object> body = new HashMap<>();
            body.put("service_id",
                    Integer.parseInt(ghnConfig.getServiceId() != null && !ghnConfig.getServiceId().isEmpty()
                            ? ghnConfig.getServiceId()
                            : "0"));
            body.put("insurance_value", 0);
            body.put("to_ward_code", wardCode);
            body.put("to_district_id", districtId);
            body.put("height", 0);
            body.put("length", 0);
            body.put("width", 0);
            body.put("weight", weight);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (resp != null && resp.getBody() != null) {
                Map<String, Object> respBody = resp.getBody();
                // GHN returns data.amount_fee and others; inspect resp shape
                Map<String, Object> data = (Map<String, Object>) respBody.get("data");
                if (data != null) {
                    Object fee = data.get("total_fee");
                    if (fee == null) {
                        // sometimes the property is 'service_fee' or 'total_price'
                        fee = data.get("service_fee");
                    }
                    double feeDouble = 0;
                    if (fee instanceof Number)
                        feeDouble = ((Number) fee).doubleValue();
                    else if (fee != null)
                        feeDouble = Double.parseDouble(String.valueOf(fee));
                    result.put("fee", feeDouble);
                    result.put("provider", "GHN");
                    result.put("svc", data.get("service_id"));
                    result.put("raw", data);
                    return result;
                } else {
                    result.put("fee", 0);
                    result.put("error", "no data in response");
                    return result;
                }
            }
        } catch (Exception e) {
            result.put("fee", 0);
            result.put("error", e.getMessage());
            result.put("provider", "GHN");
            return result;
        }
        result.put("fee", 0);
        result.put("error", "unknown");
        return result;
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getProvinces() {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty())
            return java.util.Collections.emptyList();
        try {
            String url = ghnConfig.getBaseUrl() + "/shiip/public-api/master-data/province";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    entity, java.util.Map.class);
            if (resp != null && resp.getBody() != null) {
                java.util.Map<String, Object> body = resp.getBody();
                Object data = body.get("data");
                if (data instanceof java.util.List) {
                    return (java.util.List<java.util.Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            System.err.println("GHN getProvinces failed: " + e.getMessage());
            e.printStackTrace();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getDistricts(Integer provinceId) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty() || provinceId == null)
            return java.util.Collections.emptyList();
        try {
            String url = String.format(
                    "%s/shiip/public-api/master-data/district?province_id=%d",
                    ghnConfig.getBaseUrl(), provinceId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    entity, java.util.Map.class);
            if (resp != null && resp.getBody() != null) {
                java.util.Map<String, Object> body = resp.getBody();
                Object data = body.get("data");
                if (data instanceof java.util.List) {
                    return (java.util.List<java.util.Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            System.err.println("GHN getDistricts failed: " + e.getMessage());
            e.printStackTrace();
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<java.util.Map<String, Object>> getWards(Integer districtId) {
        if (ghnConfig.getToken() == null || ghnConfig.getToken().isEmpty() || districtId == null)
            return java.util.Collections.emptyList();
        try {
            String url = String.format(
                    "%s/shiip/public-api/master-data/ward?district_id=%d", ghnConfig.getBaseUrl(), districtId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());
            HttpEntity<Void> entity = new HttpEntity<>(null, headers);
            ResponseEntity<java.util.Map> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET,
                    entity, java.util.Map.class);
            if (resp != null && resp.getBody() != null) {
                java.util.Map<String, Object> body = resp.getBody();
                Object data = body.get("data");
                if (data instanceof java.util.List) {
                    return (java.util.List<java.util.Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            System.err.println("GHN getWards failed: " + e.getMessage());
            e.printStackTrace();
        }
        return java.util.Collections.emptyList();
    }
}
