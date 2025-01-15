package com.example.google_backend.service.impl;

import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.OTPService;
import com.example.google_backend.service.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.client.OtpApiClient;
import org.opentripplanner.client.model.Coordinate;
import org.opentripplanner.client.model.RequestMode;
import org.opentripplanner.client.model.TripPlan;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class OTPServiceImpl implements OTPService {

    private final OtpApiClient client;
    private final static ZoneId ZONE_ID = ZoneId.of("Asia/Hong_Kong"); // 根据实际时区调整
    private final Logger logger = Logger.getLogger(OTPServiceImpl.class.getName());

    public OTPServiceImpl(@Value("${otp.base.url}") String otpBaseUrl) {
        this.client = new OtpApiClient(ZONE_ID, otpBaseUrl);
    }


    @Override
    public List<RouteResponse.StepDetail> getDrivingRoute(String startLoc, String endLoc) throws Exception {

        // 解析起终点坐标
        Coordinate origin = parseLocation(startLoc);
        Coordinate destination = parseLocation(endLoc);
        try {
            // 发送请求
            var response = client.plan(TripPlanParameters.builder()
                    .withFrom(origin)
                    .withTo(destination)
                    .withTime(LocalDateTime.now())
                    .withModes(Set.of(RequestMode.TRANSIT))
                    .build());

            System.out.println("OTP Response: " + response);

            // 转换结果为统一的StepDetail格式
            return convertToStepDetails(response);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get route from OTP", e);
        }
    }

    private Coordinate parseLocation(String locationJson) {
        try {
            // 解析坐标
            ObjectMapper mapper = new ObjectMapper();
            JsonNode locationNode = mapper.readTree(locationJson);

            JsonNode latLngNode = locationNode.get("latLng");
            double latitude = latLngNode.get("latitude").asDouble();
            double longitude = latLngNode.get("longitude").asDouble();

            return new Coordinate(latitude, longitude);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse location", e);
        }
    }

    private List<RouteResponse.StepDetail> convertToStepDetails(TripPlan response) {
        return null; // TODO: Implement this method
    }




}
