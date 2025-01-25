package com.example.google_backend.service.impl;

import com.example.google_backend.service.EtaService;
import com.example.google_backend.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class EtaServiceImpl implements EtaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(EtaServiceImpl.class.getName());
    //kmb bus url
    private static final String KMB_BASE_URL = "https://data.etabus.gov.hk/v1/transport/kmb/stop-eta";
    //city bus url
    private static final String CITYBUS_BASE_URL = "https://rt.data.gov.hk/v2/transport/citybus/eta/CTB";
    //mtr railway url
    private static final String MTR_BASE_URL = "https://rt.data.gov.hk/v1/transport/mtr/getSchedule.php";

    private final Map<String, Map<String, String>> stopNameToIdMap;

    @Value("classpath:kmb_stops.csv")
    private Resource kmbStopsResource;

    @Value("classpath:cb_stops.csv")
    private Resource cbStopsResource;

    public EtaServiceImpl() {
        this.stopNameToIdMap = new HashMap<>();
        this.stopNameToIdMap.put("kmb", new HashMap<>());
        this.stopNameToIdMap.put("ctb", new HashMap<>());
    }

    @PostConstruct
    public void initializeStopMaps() {
        loadKmbStops();
        loadCbStops();
    }

    private void loadKmbStops() {
        try (Reader reader = new InputStreamReader(kmbStopsResource.getInputStream());
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setQuote('"')
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : csvParser) {
                if(record.size() >= 2) {  // 确保至少有 stop 和 name_en 两列
                    String stopId = record.get(0);      // stop列
                    String nameEn = record.get(1).trim();      // name_en列
                    stopNameToIdMap.get("kmb").put(nameEn, stopId);
                }            }
        } catch (IOException e) {
            logger.error("Error loading KMB stops", e);
        }
    }

    private void loadCbStops() {
        try (Reader reader = new InputStreamReader(cbStopsResource.getInputStream());
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : csvParser) {
                if (record.size() >= 3) {  // 确保有足够的列
                    String stopId = record.get(0);    // 第一列
                    String nameEn = record.get(2).trim();  // 第三列
                    stopNameToIdMap.get("ctb").put(nameEn, stopId);
                }
            }
        } catch (IOException e) {
            logger.error("Error loading Citybus stops", e);
        }
    }

    @Override
    public List<String> getBusEta(String stopName, String routeId, String company) {
        List<String> eta = new ArrayList<>();
        String stopId = null;

        if ("kmb".equalsIgnoreCase(company)) {
            stopId = stopNameToIdMap.get("kmb").get(stopName);
            if(stopId!=null){
                eta = getKmbEta(stopId, routeId);
                logger.info("KMB ETA requested for stop name: {}, stop ID: {}", stopName, stopId);
            }else{
                logger.warn("KMB stop not found for name: {}", stopName);
            }
        } else if ("ctb".equalsIgnoreCase(company)) {
            stopId = stopNameToIdMap.get("ctb").get(stopName);
            if (stopId != null) {
                eta = getCitybusEta(stopId, routeId);
                logger.info("Citybus ETA requested for stop name: {}, stop ID: {}, route: {}",
                        stopName, stopId, routeId);
            }else {
                logger.warn("Citybus stop not found for name: {}", stopName);
            }
        } else {
            logger.warn("Unknown company identifier: {}", company);
        }

        logger.info("ETA results: {}", eta);
        return eta;
    }


    /**
     * 调用 KMB 巴士到站信息 API，获取 ETA（预计到达时间）
     *
     * @param stopId 巴士站 ID
     * @param routeId  巴士路线编号
     * @return 返回的 ETA 时间列表
     */
    private List<String> getKmbEta(String stopId,String routeId) {
        String url = KMB_BASE_URL + "/" + stopId;
        List<String> etaList = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String,Object> responseMap = JsonUtils.jsonToMap(response.getBody());
                //获取返回的data数组
                List<Map<String,Object>> dataArray = (List<Map<String, Object>>) responseMap.get("data");
                if (dataArray != null) {
                    // 过滤特定公交路线的数据
                    List<Map<String, Object>> filteredData = dataArray.stream()
                            .filter(data -> routeId.equals(data.get("route")))
                            .collect(Collectors.toList());

                    for (Map<String, Object> data : filteredData) {
                        String eta = (String) data.get("eta");
                        String destTc = (String) data.get("dest_tc");
                        String rmkTc = (String) data.get("rmk_tc");
                        String direction = (String) data.get("dir");
                        Integer serviceType = (Integer) data.get("service_type");
                        if (eta != null && !eta.isEmpty()) {
                            StringBuilder etaInfo = new StringBuilder();
                            etaInfo.append("路线: ").append(routeId)
                                    .append(", 到站时间: ").append(eta)
                                    .append(", 目的地: ").append(destTc);

                            etaList.add(etaInfo.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching KMB ETA data for stop {}: {}", stopId, e.getMessage());
        }

        return etaList;
    }

    /**
     * 调用 Citybus 巴士到站信息 API，获取 ETA（预计到达时间）
     *
     * @param stopId 巴士站 ID
     * @param routeId  巴士路线编号
     * @return 返回的 ETA 时间列表
     */
    private List<String> getCitybusEta(String stopId, String routeId) {
        List<String> etaList = new ArrayList<>();
        String url = CITYBUS_BASE_URL + "/" + stopId + "/" + routeId;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = JsonUtils.jsonToMap(response.getBody());
                List<Map<String, Object>> dataArray = (List<Map<String, Object>>) responseMap.get("data");

                if (dataArray != null) {
                    for (Map<String, Object> data : dataArray) {
                        String eta = (String) data.get("eta");
                        String destTc = (String) data.get("dest_tc");
                        Integer etaSeq = (Integer) data.get("eta_seq");


                        if (eta != null && !eta.isEmpty()) {
                            StringBuilder etaInfo = new StringBuilder();
                            etaInfo.append("城巴路线: ").append(routeId)
                                    .append(", 到站时间: ").append(eta)
                                    .append(", 目的地: ").append(destTc)
                                    .append(", 班次: ").append(etaSeq);


                            etaList.add(etaInfo.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching Citybus ETA data for stop {} and route {}: {}",
                    stopId, routeId, e.getMessage());
        }

        return etaList;
    }


}
