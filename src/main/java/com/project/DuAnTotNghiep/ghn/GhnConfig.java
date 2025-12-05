package com.project.DuAnTotNghiep.ghn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GhnConfig {
    @Value("${ghn.api.token:}")
    private String token;

    @Value("${ghn.shop.id:}")
    private String shopId;

    @Value("${ghn.service.id:}")
    private String serviceId;

    @Value("${ghn.api.base-url:https://online-gateway.ghn.vn}")
    private String baseUrl;

    public String getToken() {
        return token;
    }

    public String getShopId() {
        return shopId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
