package com.project.DuAnTotNghiep.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.DuAnTotNghiep.dto.Order.OrderDto;

import java.util.List;
import java.util.Map;

public class JsonPayloadUtils {

    public static void sanitizeEmptyStringsInMap(Map<String, Object> payloadMap) {
        if (payloadMap == null)
            return;
        for (Map.Entry<String, Object> e : new java.util.HashMap<>(payloadMap).entrySet()) {
            Object v = e.getValue();
            if (v instanceof String) {
                if (((String) v).trim().isEmpty())
                    payloadMap.put(e.getKey(), null);
            } else if (v instanceof Map) {
                sanitizeEmptyStringsInMap((Map<String, Object>) v);
            } else if (v instanceof List) {
                List<Object> lst = (List<Object>) v;
                for (int i = 0; i < lst.size(); i++) {
                    Object item = lst.get(i);
                    if (item instanceof Map) {
                        sanitizeEmptyStringsInMap((Map<String, Object>) item);
                    }
                }
            }
        }
    }

    public static OrderDto convertMapToOrderDto(ObjectMapper mapper, Map<String, Object> payloadMap) {
        if (payloadMap == null)
            return null;
        return mapper.convertValue(payloadMap, OrderDto.class);
    }
}
