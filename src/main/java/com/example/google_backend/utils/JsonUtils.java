package com.example.google_backend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 将JSON字符串转换为Map
    public static Map<String, Object> jsonToMap(String json) throws IOException {
        return objectMapper.readValue(json, Map.class);
    }

    // 将Map转换为JSON字符串
    public static String mapToJson(Map<String, Object> map) throws JsonProcessingException {
        return objectMapper.writeValueAsString(map);
    }

    // 将Object转换为JSON字符串
    public static String objectToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    // 将JSON字符串转换为Object
    public static <T> T jsonToObject(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }



    // 将JSON字符串转换为扁平化的Map
    public static Map<String, Object> flattenJson(String prefix,  Map<String, Object> map) {
        Map<String, Object> flatMap = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flatMap.putAll(flattenJson(prefix + key + ".", (Map<String, Object>) value));
            } else {
                flatMap.put(prefix + key, value);
            }
        }

        return flatMap;
    }

    // 将Object转换为Map
    public static Map<String, Object> objectToMap(Object object) throws IOException {
        return objectMapper.convertValue(object, Map.class);
    }
}
