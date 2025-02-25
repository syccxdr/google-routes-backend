package com.example.google_backend.utils.generator;


import com.example.google_backend.model.RouteRequest;

/**
 * 缓存键生成工具类
 */
public class CacheKeyGenerator {

    private CacheKeyGenerator() {
        // 私有构造函数防止实例化
    }

    /**
     * 根据路线请求生成缓存键
     *
     * @param request 路线请求
     * @return 缓存键
     */
    public static String generateRouteKey(RouteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("RouteRequest cannot be null");
        }

        StringBuilder keyBuilder = new StringBuilder("route:");

        // 处理起点坐标
        if (request.getOrigin() != null) {
            // 对坐标四舍五入到5位小数以减少微小差异造成的缓存未命中
            // 5位小数精度约为1.1米，足够大多数城市导航场景
            keyBuilder.append(roundCoordinate(request.getOrigin().getLatitude()))
                    .append(",")
                    .append(roundCoordinate(request.getOrigin().getLongitude()));
        } else {
            keyBuilder.append("unknown");
        }

        keyBuilder.append(":");

        // 处理终点坐标
        if (request.getDestination() != null) {
            keyBuilder.append(roundCoordinate(request.getDestination().getLatitude()))
                    .append(",")
                    .append(roundCoordinate(request.getDestination().getLongitude()));
        } else {
            keyBuilder.append("unknown");
        }

        keyBuilder.append(":");

        // 添加出行方式
        keyBuilder.append(request.getTravelMode() != null ? request.getTravelMode() : "ANY");

        // 添加公共交通方式
        if (request.getTransitModes() != null && !request.getTransitModes().isEmpty()) {
            keyBuilder.append(":");
            keyBuilder.append(String.join(",", request.getTransitModes()));
        }

        return keyBuilder.toString();
    }

    private static double roundCoordinate(double coordinate) {
        // 保留5位小数
        return Math.round(coordinate * 1e5) / 1e5;

    }

    // 可以添加其他类型的缓存键生成方法
    public static String generateUserKey(String userId) {
        return "user:" + userId;
    }
}
