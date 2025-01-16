package com.example.google_backend.service.impl;

import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.OTPService;
import com.example.google_backend.service.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.client.OtpApiClient;
import org.opentripplanner.client.model.*;
import org.opentripplanner.client.parameters.TripPlanParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
                    .withModes(Set.of(RequestMode.CAR))
                    .build());

            System.out.println(" OTP response : " + response);

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
        List<RouteResponse.StepDetail> steps = new ArrayList<>();

        //检查响应是否为空
        if(response == null || response.itineraries() == null || response.itineraries().isEmpty()) {
            return steps;
        }

        // 获取第一个行程的第一个leg
        Itinerary firstItinerary = response.itineraries().getFirst();

        if(!firstItinerary.legs().isEmpty()){
            Leg leg = firstItinerary.legs().getFirst();
            RouteResponse.StepDetail step = new RouteResponse.StepDetail();

            //设置行进模式为DRIVE
            step.setTravelMode("DRIVE");
            //设置距离
            step.setDistance((long) leg.distance());

            // 处理headsign - 如果为空就不设置或设置为null
            leg.headsign().ifPresent(step::setHeadsign);

            // 设置路线指示
            String instruction = String.format("OTP result From %s drive to %s",
                    leg.from().name(),
                    leg.to().name());
            step.setInstruction(instruction);

            //设置时间,返回为秒数
            step.setDuration(leg.duration().getSeconds());


            steps.add(step);

        }

        return steps;


    }




}
