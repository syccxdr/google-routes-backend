package com.example.google_backend.utils.route;


import com.example.google_backend.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.opentripplanner.client.model.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Component
public class TaxiHotSpotChecker {
    private static final List<TaxiHotspot> hotspots = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(TaxiHotSpotChecker.class.getName());

    @Value("${taxi.data.hotspots-file}")
    private Resource hotspotsResource;

    @Data
    @AllArgsConstructor
    private static class TaxiHotspot {
        private double latMin;
        private double latMax;
        private double lngMin;
        private double lngMax;
        private double count;
        private double centerLat;
        private double centerLng;
    }

    @PostConstruct
    public void init() {
        loadTaxiHotspotData();
    }

    private void loadTaxiHotspotData() {
        long startTime = System.currentTimeMillis();
        // 先检查资源是否存在
        if (!hotspotsResource.exists()) {
            logger.error("热点数据文件不存在: " + hotspotsResource.getDescription());
            return;
        }

        try (Reader reader = new InputStreamReader(hotspotsResource.getInputStream());
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : csvParser) {
                TaxiHotspot hotspot = new TaxiHotspot(
                        Double.parseDouble(record.get("lat_min")),
                        Double.parseDouble(record.get("lat_max")),
                        Double.parseDouble(record.get("lng_min")),
                        Double.parseDouble(record.get("lng_max")),
                        Double.parseDouble(record.get("count")),
                        Double.parseDouble(record.get("center_lat")),
                        Double.parseDouble(record.get("center_lng"))
                );

                // 过滤掉异常数据（经纬度为0的记录）
                if (hotspot.getLatMin() > 0 && hotspot.getLngMin() > 0) {
                    hotspots.add(hotspot);
                }
            }

        } catch (IOException e) {
            logger.error("加载出租车热点数据CSV文件失败");
        }

        logger.info("加载出租车热点数据完成，共 {} 个热点区域，耗时：{}ms",
                hotspots.size(),
                System.currentTimeMillis() - startTime);
    }

    public boolean isHotspot(JsonNode transitDetails) throws IOException {

        if(transitDetails == null) return false;

        JsonNode stopDetails = transitDetails.path("stopDetails");
        if (stopDetails.isMissingNode()) return false;

        //解析站点经纬度，首先得到json字符串
        String startStopLocation = stopDetails.path("departureStop")
                .path("location").toString();

        // 解析坐标
        ObjectMapper mapper = new ObjectMapper();
        JsonNode locationNode = mapper.readTree(startStopLocation);

        JsonNode latLngNode = locationNode.get("latLng");
        double lat = latLngNode.get("latitude").asDouble();
        double lng = latLngNode.get("longitude").asDouble();

        try {
            // 检查该位置是否在任何一个热点区域内
            for (TaxiHotspot hotspot : hotspots) {
                if (lat >= hotspot.getLatMin() && lat <= hotspot.getLatMax() &&
                        lng >= hotspot.getLngMin() && lng <= hotspot.getLngMax()) {
                    logger.info("发现打车热点区域: ({}, {}), 该区域出租车数量: {}",
                            hotspot.getCenterLat(),
                            hotspot.getCenterLng(),
                            hotspot.getCount());
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("检查打车热点状态时发生错误", e);
        }

        return false;
    }
}
