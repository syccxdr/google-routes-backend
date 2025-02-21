package com.example.google_backend.utils.route;


import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.impl.EtaServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CongestionStationChecker {

    private static final Map<String, Set<String>> timeLocationMap = new HashMap<>();
//    private static final double CONGESTION_THRESHOLD = 5000.0;
    private static final Logger logger = LoggerFactory.getLogger(CongestionStationChecker.class.getName());

    @Value("${congestion.data.bus-stops-file}")
    private Resource busStationResource;

    @Value("${congestion.data.mtr-stops-file}")
    private Resource mtrStationResource;

    @PostConstruct
    public void init() {
        loadBusCongestionData();
    }

    private void loadBusCongestionData() {
        long startTime = System.currentTimeMillis();

        try (Reader reader = new InputStreamReader(busStationResource.getInputStream());
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : csvParser) {
                String time = record.get(1).trim();
                String location = record.get(2).trim();
                double congestionValue = Double.parseDouble(record.get(3).trim());

                // 使用Set存储站点名称
                timeLocationMap.computeIfAbsent(time, k -> new HashSet<>())
                        .add(location);
            }

        } catch (IOException e) {
            logger.error("加载公交拥堵数据CSV文件失败", e);
        }

        logger.info("加载公交拥堵数据完成，共 {} 个时间点，耗时：{}ms",
                timeLocationMap.size(),
                System.currentTimeMillis() - startTime);
    }

    public static boolean isCongested(JsonNode transitDetails) {

        if (transitDetails == null) return false;

        // 获取交通工具信息
        JsonNode transitLine = transitDetails.path("transitLine");
        if (transitLine.isMissingNode()) return false;

        // 获取交通工具类型
        String vehicleType = transitLine.path("vehicle")
                .path("type")
                .asText("");


        // 暂时只判断公交车
        if ("BUS".equals(vehicleType)) {
            try {
                JsonNode stopDetails = transitDetails.path("stopDetails");
                if (stopDetails.isMissingNode()) return false;
                // 获取当前出发站点名称
                String stationName = stopDetails.path("departureStop")
                        .path("name")
                        .asText();

                // 获取时间 HH:mm 格式 并 将其转换为 HK 时间
                String departureTime = stopDetails.path("departureTime")
                        .asText();
                String currentTime = ZonedDateTime.parse(departureTime)
                        .withZoneSameInstant(ZoneId.of("Asia/Hong_Kong"))
                        .format(DateTimeFormatter.ofPattern("HH:mm"));


                // 检查是否存在拥堵站点
                Set<String> locations = timeLocationMap.get(currentTime);
                if (locations != null && locations.contains(stationName)) {
                    logger.info("发现拥堵公交站点: {}, 时间点: {}", stationName, currentTime);
                    return true;

                }

                return false;
            } catch (Exception e) {
                logger.error("检查站点拥堵状态时发生错误", e);
                return false;
            }
        }

        return false;
    }
}