package com.example.google_backend.utils.route;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CrossSeaRouteChecker {
    private static final Set<String> CROSS_SEA_MTR_LINES = Set.of(
            "Tsuen Wan Line",
            "Tseung Kwan O Line",
            "Tung Chung Line",
            "East Rail Line"
    );

    private static final Set<String> CROSS_SEA_BUS_ROUTES = Set.of(
            // 红隧和东隧路线
            "101", "101X", "102", "102P", "103", "104",
            "106", "106P", "107", "107P", "108", "109",
            "110", "111", "111P", "112", "113", "115",
            "116", "117", "118", "118P", "170", "171",
            "171A", "171P", "182", "373", "N118", "N121",
            "N122", "N170", "N171", "N182", "N368", "N373",
            "H2", "H2K", "N11",
            // 西隧路线
            "930", "930A", "930X", "933", "950", "952",
            "952C", "955", "962", "962C", "962G", "962X",
            "967", "967X", "969", "969A", "969B", "969C",
            "970", "970X", "971", "973", "976", "976A",
            "979", "987", "988", "A10", "A11", "A12",
            "A17", "E11", "E11A", "E11B", "N930", "N952",
            "N962", "N969", "NA10", "NA11", "NA12", "X962",
            "X970"
    );

    public static boolean isCrossSeaRoute(JsonNode transitDetails) {
        if (transitDetails == null) return false;

        // 获取交通工具信息
        JsonNode transitLine = transitDetails.path("transitLine");
        if (transitLine.isMissingNode()) return false;

        // 获取交通工具类型
        String vehicleType = transitLine.path("vehicle")
                .path("type")
                .asText("");

        // 获取路线名称或编号
        String routeName = transitLine.path("nameShort").asText("");
        if (routeName.isEmpty()) {
            routeName = transitLine.path("name").asText("");
        }

        // 根据交通工具类型判断
        if ("BUS".equals(vehicleType)) {
            return CROSS_SEA_BUS_ROUTES.contains(routeName);
        } else if ("SUBWAY".equals(vehicleType) || "HEAVY_RAIL".equals(vehicleType)) {
            return CROSS_SEA_MTR_LINES.contains(routeName);
        }

        return false;
    }
}
