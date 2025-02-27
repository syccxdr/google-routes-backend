package com.example.google_backend.utils.route;

import com.example.google_backend.common.redis.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
public class LongDistanceExitChecker {

    private static final Logger logger = LoggerFactory.getLogger(LongDistanceExitChecker.class);

    // 定义常量
    private static final String REDIS_GEO_KEY = "metro:exits";
    private static final String REDIS_DISTANCE_KEY = "metro:exit:distances";
    private static final double LONG_DISTANCE_THRESHOLD = 600.0; // 长距离出口阈值(米)
    private static final double SEARCH_RADIUS = 50.0; // 搜索半径(米)

    @Value("${exits.data.long-distance-file}")
    private Resource longDistanceExitsResource;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        loadLongDistanceExitData();
    }

    /**
     * 加载长距离出口数据到Redis
     */
    private void loadLongDistanceExitData() {
        long startTime = System.currentTimeMillis();

        try (Reader reader = new InputStreamReader(longDistanceExitsResource.getInputStream());
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            // 准备批量添加到Redis的数据
            Map<String, String> exitDistanceMap = new HashMap<>();

            int count = 0;
            for (CSVRecord record : csvParser) {
                try {
                    String exitFullName = record.get("Name-Exit");
                    double longitude = Double.parseDouble(record.get("longitude"));
                    double latitude = Double.parseDouble(record.get("latitude"));
                    double distance = Double.parseDouble(record.get("distance"));

                    // 添加地理位置信息
                    redisService.geoAdd(REDIS_GEO_KEY, longitude, latitude, exitFullName);

                    // 存储出口距离信息
                    exitDistanceMap.put(exitFullName, String.valueOf(distance));

                    count++;
                } catch (Exception e) {
                    logger.warn("处理CSV记录时出错: {}", e.getMessage());
                }
            }

            // 批量添加距离信息到Redis
            if (!exitDistanceMap.isEmpty()) {
                // 为避免重复数据，先删除旧键
                redisService.deleteObject(REDIS_GEO_KEY);
                redisService.deleteObject(REDIS_DISTANCE_KEY);

                redisService.setCacheMap(REDIS_DISTANCE_KEY, exitDistanceMap);
            }

            logger.info("成功加载 {} 个地铁出口到Redis，耗时：{}ms",
                    count,
                    System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            logger.error("加载长距离出口CSV文件失败", e);
        }
    }

    /**
     * 检查步骤节点是否指向长距离出口
     * @param transitDetails JsonNode 步骤节点
     * @return 如果是长距离出口返回true，否则返回false
     */
    public boolean isLongDistanceExitStep(JsonNode transitDetails) {
        if (transitDetails == null) return false;

        try {
            // 提取步骤的终点位置
            JsonNode stopDetails = transitDetails.path("stopDetails");
            if (stopDetails.isMissingNode()) return false;

            //解析站点经纬度，首先得到经纬度json字符串
            String startStopLocation = stopDetails.path("departureStop")
                    .path("location").toString();

            // 解析坐标
            ObjectMapper mapper = new ObjectMapper();
            JsonNode locationNode = mapper.readTree(startStopLocation);

            JsonNode latLngNode = locationNode.get("latLng");
            double lat = latLngNode.get("latitude").asDouble();
            double lng = latLngNode.get("longitude").asDouble();

            // 使用经纬度检查是否为长距离出口
            return isLongDistanceExit(lat, lng);

        } catch (Exception e) {
            logger.error("解析步骤节点时发生错误", e);
            return false;
        }
    }

    /**
     * 检查给定位置是否为长距离出口
     * @param latitude 纬度
     * @param longitude 经度
     * @return 如果是长距离出口返回true，否则返回false
     */
    public boolean isLongDistanceExit(double latitude, double longitude) {
        try {
            // 创建查询参数
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(1);

            // 执行地理位置查询 - 直接使用RedisTemplate因为RedisService可能没有完全实现所需方法
            var results = redisTemplate.opsForGeo().radius(
                    REDIS_GEO_KEY,               // key
                    new Circle(                  // circle
                            new Point(longitude, latitude),  // center
                            new Distance(SEARCH_RADIUS)      // radius
                    ),
                    args                         // command args
            );

            if (results != null && !results.getContent().isEmpty()) {
                var result = results.getContent().get(0);
                String exitName = result.getContent().getName();

                // 获取出口到站点的距离
                String distanceStr = redisService.getCacheMapValue(REDIS_DISTANCE_KEY, exitName);
                if (distanceStr != null) {
                    double distanceToStation = Double.parseDouble(distanceStr);

                    // 判断是否为长距离出口
                    if (distanceToStation > LONG_DISTANCE_THRESHOLD) {
                        logger.info("检测到长距离出口: {}, 距离月台: {}米", exitName, distanceToStation);
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            logger.error("检查长距离出口时发生错误", e);
            return false;
        }
    }

    /**
     * 获取长距离出口信息
     * @param latitude 纬度
     * @param longitude 经度
     * @return 如果是长距离出口返回出口名称，否则返回null
     */
    public String getLongDistanceExitName(double latitude, double longitude) {
        try {
            // 创建查询参数
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .sortAscending()
                    .limit(1);

            // 执行地理位置查询
            Point center = new Point(longitude, latitude);
            // 执行地理位置查询
            var results = redisTemplate.opsForGeo().radius(
                    REDIS_GEO_KEY,               // key
                    new Circle(                  // circle
                            new Point(longitude, latitude),  // center
                            new Distance(SEARCH_RADIUS)      // radius
                    ),
                    args                         // command args
            );

            if (results != null && !results.getContent().isEmpty()) {
                var result = results.getContent().getFirst();
                String exitName = result.getContent().getName();

                // 获取出口到站点的距离
                String distanceStr = redisService.getCacheMapValue(REDIS_DISTANCE_KEY, exitName);
                if (distanceStr != null) {
                    double distanceToStation = Double.parseDouble(distanceStr);

                    // 判断是否为长距离出口
                    if (distanceToStation > LONG_DISTANCE_THRESHOLD) {
                        return exitName;
                    }
                }
            }

            return null;
        } catch (Exception e) {
            logger.error("获取长距离出口信息时发生错误", e);
            return null;
        }
    }
}