package com.example.google_backend.service.impl;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.RouteService;
import com.example.google_backend.model.RouteRequestPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RouteServiceImpl implements RouteService {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = Logger.getLogger(RouteService.class.getName());


    public JsonNode getResponse(RouteRequest request) throws Exception {
        // Step 1: Prepare the computeRoutes API request URL
        String computeRoutesUrl = "https://routes.googleapis.com/directions/v2:computeRoutes?key=" + googleApiKey;

        // Step 2: Build the request payload as per Google Routes API specifications
        RouteRequestPayload payload = buildPayload(request);
        payload.setComputeAlternativeRoutes(true);

        // Convert payload to JSON
        String requestBody = objectMapper.writeValueAsString(payload);

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-FieldMask", "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs");

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Step 3: Call the computeRoutes API and measure the time taken
        Instant startTime = Instant.now();
        ResponseEntity<JsonNode> responseEntity;
        try {
            responseEntity = restTemplate.exchange(computeRoutesUrl, HttpMethod.POST, entity, JsonNode.class);
        } catch (Exception e) {
            logger.severe("Error calling Google Routes API: " + e.getMessage());
            throw new Exception("Error calling Google Routes API", e);
        }
        Instant endTime = Instant.now();

        long timeElapsed = Duration.between(startTime, endTime).toMillis(); // Time in milliseconds
        logger.info("Time taken to fetch routes: " + timeElapsed + " ms");

        JsonNode computeRoutesResponse = responseEntity.getBody();
        return computeRoutesResponse;
    }

    public RouteResponse.StepDetail setStepDetail(JsonNode stepNode){
        RouteResponse.StepDetail stepDetail = new RouteResponse.StepDetail();

        stepDetail.setInstruction(
                stepNode.has("navigationInstruction")
                        && stepNode.get("navigationInstruction").has("instructions")
                        ? stepNode.get("navigationInstruction").get("instructions").asText()
                        : "No Instruction"
        );
        stepDetail.setDistance(
                stepNode.has("distanceMeters")
                        ? stepNode.get("distanceMeters").asLong()
                        : 0L
        );
        stepDetail.setDuration(
                stepNode.has("staticDuration")
                        ? parseDuration(stepNode.get("staticDuration").asText())
                        : 0L
        );
        stepDetail.setPolyline(
                stepNode.has("polyline") && !stepNode.get("polyline").isNull()
                        && stepNode.get("polyline").has("encodedPolyline")
                        ? stepNode.get("polyline").get("encodedPolyline").asText()
                        : "No Polyline"
        );
        String stepTravelMode=stepNode.has("travelMode")
                ? stepNode.get("travelMode").asText()
                : "WALK";
        stepDetail.setTravelMode(
                stepTravelMode
        );
        return stepDetail;

    }

    public List<RouteResponse.StepDetail> changeStep(String startLoc, String endLoc) {
        return new ArrayList<>();
    }


    /**
     * Retrieves routes based on the provided request.
     *
     * @param request The route calculation request.
     * @return The route response containing route details.
     * @throws Exception if an error occurs during API calls.
     */
    public RouteResponse getRoutes(RouteRequest request) throws Exception {
        JsonNode computeRoutesResponse = getResponse(request);
        if (computeRoutesResponse == null || !computeRoutesResponse.has("routes")) {
            throw new Exception("Unable to retrieve routes from Google Routes API.");
        }

        List<RouteResponse.RouteDetail> routeDetails = new ArrayList<>();
        for (JsonNode routeNode : computeRoutesResponse.get("routes")) {
            String summary = routeNode.has("summary") && !routeNode.get("summary").isNull()
                    ? routeNode.get("summary").asText() : "No Summary";
            String distanceMeters = routeNode.has("distanceMeters") && !routeNode.get("distanceMeters").isNull()
                    ? routeNode.get("distanceMeters").asText() : "Unknown";
            String duration = routeNode.has("duration") && !routeNode.get("duration").isNull()
                    ? routeNode.get("duration").asText() : "Unknown";
            String polyline = "No Polyline";
            // Extract polyline if available
            if (routeNode.has("polyline") && !routeNode.get("polyline").isNull()
                    && routeNode.get("polyline").has("encodedPolyline")
                    && !routeNode.get("polyline").get("encodedPolyline").isNull()) {
                polyline = routeNode.get("polyline").get("encodedPolyline").asText();
            }
            // Initialize RouteDetail object
            RouteResponse.RouteDetail routeDetail = new RouteResponse.RouteDetail();
            routeDetail.setSummary(summary);
            routeDetail.setDistanceMeters(distanceMeters);
            routeDetail.setDuration(duration);
            routeDetail.setPolyline(polyline);

            // 初始化 legs 容器
            List<RouteResponse.LegDetail> legs = new ArrayList<>();

            if (routeNode.has("legs") && !routeNode.get("legs").isNull()) {
                for (JsonNode legNode : routeNode.get("legs")) {
                    RouteResponse.LegDetail legDetail = new RouteResponse.LegDetail();
                    // Extract start and end locations
                    if (legNode.has("startLocation") && !legNode.get("startLocation").isNull()) {
                        legDetail.setStartLocation(legNode.get("startLocation").toString());
                    } else {
                        legDetail.setStartLocation("Unknown Start Location");
                    }
                    if (legNode.has("endLocation") && !legNode.get("endLocation").isNull()) {
                        legDetail.setEndLocation(legNode.get("endLocation").toString());
                    } else {
                        legDetail.setEndLocation("Unknown End Location");
                    }
                    // 如果 routeRequest 是 TRANSIT，就设置 travelMode=TRANSIT，否则按请求值
                    if ("TRANSIT".equalsIgnoreCase(request.getTravelMode())) {
                        legDetail.setTravelMode("TRANSIT");
                    } else {
                        legDetail.setTravelMode(request.getTravelMode());
                    }

                    // distance/duration
                    legDetail.setDistance(
                            legNode.has("distanceMeters") && !legNode.get("distanceMeters").isNull()
                                    ? legNode.get("distanceMeters").asText()
                                    : "Unknown Distance"
                    );
                    legDetail.setDuration(
                            legNode.has("duration") && !legNode.get("duration").isNull()
                                    ? legNode.get("duration").asText()
                                    : "Unknown Duration"
                    );

                    // 这里同理拆分 steps
                    List<RouteResponse.StepDetail> steps = new ArrayList<>();
                    boolean needReplacement = false;

                    String previousTravelMode="WALK";
                    long previousWalkDuration = 0;
                    Instant curStepArrivalTime = Instant.now();
                    Instant previousStepArrivalTime = Instant.now();
                    if (legNode.has("steps") && !legNode.get("steps").isNull()) {
                        for (int i = 0; i < legNode.get("steps").size(); i++) {
                            JsonNode stepNode = legNode.get("steps").get(i);
                            RouteResponse.StepDetail stepDetail = setStepDetail(stepNode);
                            String stepTravelMode=stepNode.has("travelMode")
                                    ? stepNode.get("travelMode").asText()
                                    : "WALK";
                            logger.info("stepTravelMode"+stepTravelMode+";"+"getDuration"+stepDetail.getDuration());

                            if("WALK".equalsIgnoreCase(previousTravelMode) && "WALK".equalsIgnoreCase(stepTravelMode)) {
                                previousWalkDuration+=stepDetail.getDuration();
                            }
                            if ("TRANSIT".equalsIgnoreCase(stepTravelMode)) {
                                if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()) {
                                    JsonNode transitDetails = stepNode.get("transitDetails");
                                    RouteResponse.StepDetail.TransitDetails td = parseTransitDetails(transitDetails);
                                    if (td.getStopDetails() != null
                                            && td.getStopDetails().getArrivalTime() != null) {
                                        curStepArrivalTime = Instant.parse(td.getStopDetails().getArrivalTime());
                                        logger.info("departureTime: " + td.getStopDetails().getArrivalTime());
                                        logger.info("arrivalTime: " + td.getStopDetails().getDepartureTime());
                                    }
                                    if("WALK".equalsIgnoreCase(previousTravelMode)){
                                        // ② 同样计算"等待时间"：前一个 step 的结束时间 + 走到站的duration => 当前 Bus 的 arrivalTime
                                        if (td.getStopDetails() != null
                                                && td.getStopDetails().getArrivalTime() != null) {
                                            String departureTimeStr = td.getStopDetails().getDepartureTime();
                                            try {
                                                Instant departureTime = Instant.parse(departureTimeStr);
                                                // 步行到这里的持续时间
                                                long walkDuration = previousWalkDuration;
                                                Instant walkToStationTime = previousStepArrivalTime
                                                        .plusSeconds(walkDuration);
                                                long waitTimeSeconds = Duration
                                                        .between(walkToStationTime, departureTime).getSeconds();
                                                logger.info("walkToStationTime:"+walkToStationTime+"=previousStepArrivalTime:"+previousStepArrivalTime+"+walkDuration:"+walkDuration);
                                                logger.info("waitTimeSeconds:"+waitTimeSeconds+"=walkToStationTime:"+walkToStationTime+"-departureTime:"+departureTime);
                                                if (waitTimeSeconds > 2) {
                                                    String startLoc= td.getStopDetails().getDepartureStop().getLocation();
                                                    String endLoc="";
                                                    logger.info("等待时间"+waitTimeSeconds+"超过20min，重新调用Google Routes API, 切换本段step为Drive");
                                                    logger.info("i:"+i);
                                                    while("TRANSIT".equalsIgnoreCase(stepTravelMode) && i < legNode.get("steps").size()){
                                                        stepNode = legNode.get("steps").get(i);
                                                        stepDetail = setStepDetail(stepNode);
                                                        stepTravelMode=stepNode.has("travelMode")
                                                                ? stepNode.get("travelMode").asText()
                                                                : "WALK";
                                                        if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()){
                                                            JsonNode curtransitDetails = stepNode.get("transitDetails");
                                                            RouteResponse.StepDetail.TransitDetails curtd = parseTransitDetails(curtransitDetails);
                                                            stepDetail.setTransitDetails(curtd);

                                                            if (curtd.getStopDetails() != null) {
                                                                endLoc = curtd.getStopDetails().getArrivalStop().getLocation();
                                                            }
                                                        }
                                                        System.out.println("i:"+"start:"+startLoc+"end"+endLoc);
                                                        i++;

                                                    }
                                                    logger.info("start: " + startLoc + " end: " + endLoc);
                                                    // 调用 changeStep 获取 driving 步骤
                                                    List<RouteResponse.StepDetail> changDetails = changeStep(startLoc, endLoc);
                                                    steps.addAll(changDetails);
                                                    needReplacement = true;
                                                }
                                            } catch (Exception e) {
                                                System.out.println(e);
                                            }
                                        }
                                    } else {
                                        stepDetail.setTransitDetails(td);
                                    }
                                }
                            }
                            previousStepArrivalTime = curStepArrivalTime;
                            previousTravelMode = stepTravelMode;
                            if(needReplacement == false){
                                steps.add(stepDetail);
                                System.out.println("i:"+i+"stepdetails"+stepTravelMode);
                            }
                            needReplacement = false;
                        }
                    }
                    legDetail.setSteps(steps);
                    legs.add(legDetail);
                }
            }
            routeDetail.setLegs(legs);
            routeDetails.add(routeDetail);
        }

        RouteResponse response = new RouteResponse();
        response.setRoutes(routeDetails);
        return response;
    }

    @Override
    public List<RouteResponse.RouteDetail> sortRoutes(List<RouteResponse.RouteDetail> allRoutes, String sortType) {

        if (allRoutes == null || allRoutes.isEmpty()) {
            throw new IllegalArgumentException("No routes to sort.");
        }
        switch (sortType) {
            case "fewestTransfers":
                return allRoutes.stream()
                        .sorted(Comparator.comparingInt(route -> route.getLegs().size())) // 按换乘次数升序
                        .collect(Collectors.toList());

            case "lowestPrice":
                //TODO: Implement sorting by price
                return new ArrayList<>(allRoutes);

            case "shortestDuration":
                return allRoutes.stream()
                        .sorted(Comparator.comparingLong(route -> parseDuration(route.getDuration()))) // 按时间升序
                        .collect(Collectors.toList());

            case "shortestDistance":
                return allRoutes.stream()
                        .sorted(Comparator.comparingInt(route -> Integer.parseInt(route.getDistanceMeters()))) // 按距离升序
                        .collect(Collectors.toList());

            default:
                throw new IllegalArgumentException("Invalid sort type: " + sortType);
        }

    }

    /**
     * Parses the transit details from the JSON node.
     *
     * @param transitDetailsNode The JSON node containing transit details.
     * @return The populated TransitDetails object.
     */
    private RouteResponse.StepDetail.TransitDetails parseTransitDetails(JsonNode transitDetailsNode) {
        RouteResponse.StepDetail.TransitDetails transitDetails = new RouteResponse.StepDetail.TransitDetails();

        // Parse stopDetails
        if (transitDetailsNode.has("stopDetails") && !transitDetailsNode.get("stopDetails").isNull()) {
            JsonNode stopDetailsNode = transitDetailsNode.get("stopDetails");
            RouteResponse.StepDetail.TransitDetails.StopDetails stopDetails = new RouteResponse.StepDetail.TransitDetails.StopDetails();

            if (stopDetailsNode.has("arrivalStop") && !stopDetailsNode.get("arrivalStop").isNull()) {
                JsonNode arrivalStopNode = stopDetailsNode.get("arrivalStop");
                RouteResponse.StepDetail.TransitDetails.StopDetails.Stop arrivalStop = new RouteResponse.StepDetail.TransitDetails.StopDetails.Stop();
                arrivalStop.setName(arrivalStopNode.has("name") ? arrivalStopNode.get("name").asText() : "Unknown Arrival Stop");
                if (arrivalStopNode.has("location") && !arrivalStopNode.get("location").isNull()) {
                    arrivalStop.setLocation(arrivalStopNode.get("location").toString());
                } else {
                    arrivalStop.setLocation("Unknown Location");
                }
                stopDetails.setArrivalStop(arrivalStop);
            }

            if (stopDetailsNode.has("departureStop") && !stopDetailsNode.get("departureStop").isNull()) {
                JsonNode departureStopNode = stopDetailsNode.get("departureStop");
                RouteResponse.StepDetail.TransitDetails.StopDetails.Stop departureStop = new RouteResponse.StepDetail.TransitDetails.StopDetails.Stop();
                departureStop.setName(departureStopNode.has("name") ? departureStopNode.get("name").asText() : "Unknown Departure Stop");
                if (departureStopNode.has("location") && !departureStopNode.get("location").isNull()) {
                    departureStop.setLocation(departureStopNode.get("location").toString());
                } else {
                    departureStop.setLocation("Unknown Location");
                }
                stopDetails.setDepartureStop(departureStop);
            }

            transitDetails.setStopDetails(stopDetails);
        }

        // Parse headsign
        if (transitDetailsNode.has("headsign") && !transitDetailsNode.get("headsign").isNull()) {
            transitDetails.setHeadsign(transitDetailsNode.get("headsign").asText());
        }

        // Parse transitLine
        if (transitDetailsNode.has("transitLine") && !transitDetailsNode.get("transitLine").isNull()) {
            JsonNode transitLineNode = transitDetailsNode.get("transitLine");
            RouteResponse.StepDetail.TransitDetails.TransitLine transitLine = new RouteResponse.StepDetail.TransitDetails.TransitLine();

            // Agencies
            if (transitLineNode.has("agencies") && transitLineNode.get("agencies").isArray()) {
                List<RouteResponse.StepDetail.TransitDetails.TransitLine.Agency> agencies = new ArrayList<>();
                for (JsonNode agencyNode : transitLineNode.get("agencies")) {
                    RouteResponse.StepDetail.TransitDetails.TransitLine.Agency agency = new RouteResponse.StepDetail.TransitDetails.TransitLine.Agency();
                    agency.setName(agencyNode.has("name") ? agencyNode.get("name").asText() : null);
                    agency.setPhoneNumber(agencyNode.has("phoneNumber") ? agencyNode.get("phoneNumber").asText() : null);
                    agency.setUri(agencyNode.has("uri") ? agencyNode.get("uri").asText() : null);
                    agencies.add(agency);
                }
                transitLine.setAgencies(agencies);
            }

            // Other transitLine fields
            transitLine.setName(transitLineNode.has("name") ? transitLineNode.get("name").asText() : null);
            transitLine.setColor(transitLineNode.has("color") ? transitLineNode.get("color").asText() : null);
            transitLine.setNameShort(transitLineNode.has("nameShort") ? transitLineNode.get("nameShort").asText() : null);
            transitLine.setTextColor(transitLineNode.has("textColor") ? transitLineNode.get("textColor").asText() : null);

            // Vehicle
            if (transitLineNode.has("vehicle") && !transitLineNode.get("vehicle").isNull()) {
                JsonNode vehicleNode = transitLineNode.get("vehicle");
                RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle vehicle = new RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle();

                if (vehicleNode.has("name") && !vehicleNode.get("name").isNull()) {
                    RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle.Name name = new RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle.Name();
                    name.setText(vehicleNode.get("name").has("text") ? vehicleNode.get("name").get("text").asText() : null);
                    vehicle.setName(name);
                }
                vehicle.setType(vehicleNode.has("type") ? vehicleNode.get("type").asText() : null);
                vehicle.setIconUri(vehicleNode.has("iconUri") ? vehicleNode.get("iconUri").asText() : null);
                transitLine.setVehicle(vehicle);
            }

            transitDetails.setTransitLine(transitLine);
        }

        // Parse stopCount
        if (transitDetailsNode.has("stopCount")) {
            transitDetails.setStopCount(transitDetailsNode.get("stopCount").asInt());
        }

        // Parse localizedValues if needed (optional)

        return transitDetails;
    }

    /**
     * Parses duration string (e.g., "1581s") to seconds.
     *
     * @param durationStr The duration string.
     * @return The duration in seconds.
     */
    private long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        durationStr = durationStr.trim().toLowerCase();
        if (durationStr.endsWith("s")) {
            try {
                return Long.parseLong(durationStr.replace("s", ""));
            } catch (NumberFormatException e) {
                logger.warning("Failed to parse duration: " + durationStr);
                return 0;
            }
        }
        return 0;
    }

    /**
     * Builds the request payload for Google Routes API.
     *
     * @param request The route calculation request from frontend.
     * @return The structured payload as per API requirements.
     */
    private RouteRequestPayload buildPayload(RouteRequest request) {
        RouteRequestPayload payload = new RouteRequestPayload();

        // Set Origin
        RouteRequestPayload.Origin origin = new RouteRequestPayload.Origin();
        origin.setLocation(new RouteRequestPayload.Location(
                new RouteRequestPayload.LatLng(request.getOrigin().getLatitude(), request.getOrigin().getLongitude())
        ));
        payload.setOrigin(origin);

        // Set Destination
        RouteRequestPayload.Destination destination = new RouteRequestPayload.Destination();
        destination.setLocation(new RouteRequestPayload.Location(
                new RouteRequestPayload.LatLng(request.getDestination().getLatitude(), request.getDestination().getLongitude())
        ));
        payload.setDestination(destination);

        // Set Travel Mode
        payload.setTravelMode(request.getTravelMode());

        // Set Compute Alternative Routes
        payload.setComputeAlternativeRoutes(true);

        // Set Transit Preferences if Travel Mode is TRANSIT
        if ("TRANSIT".equalsIgnoreCase(request.getTravelMode()) && request.getTransitModes() != null && !request.getTransitModes().isEmpty()) {
            RouteRequestPayload.TransitPreferences transitPreferences = new RouteRequestPayload.TransitPreferences();
            List<String> preferredModes = new ArrayList<>();
            for (String mode : request.getTransitModes()) {
                // Google Routes API expects modes in uppercase and specific format
                preferredModes.add(mode.toUpperCase());
            }
            transitPreferences.setAllowedTravelModes(preferredModes);
            payload.setTransitPreferences(transitPreferences);
        }

        return payload;
    }
}
