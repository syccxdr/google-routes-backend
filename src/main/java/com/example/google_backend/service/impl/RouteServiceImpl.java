package com.example.google_backend.service.impl;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.OTPService;
import com.example.google_backend.service.RouteService;
import com.example.google_backend.model.RouteRequestPayload;
import com.example.google_backend.utils.TimingUtils;
import com.example.google_backend.utils.route.CrossSeaRouteChecker;
import com.example.google_backend.utils.route.CongestionStationChecker;
import com.example.google_backend.utils.route.TaxiHotSpotChecker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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

    @Resource
    private OTPService otpService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = Logger.getLogger(RouteServiceImpl.class.getName());

    private static final long MAX_WAIT_TIME_SECONDS = 1200;



    public JsonNode getResponse(RouteRequest request) throws Exception {
        // Step 1: Prepare the computeRoutes API request URL
        String computeRoutesUrl = "https://routes.googleapis.com/directions/v2:computeRoutes";

        // Step 2: Build the request payload as per Google Routes API specifications
        RouteRequestPayload payload = buildPayload(request);
        payload.setComputeAlternativeRoutes(true);

        // Convert payload to JSON
        String requestBody = objectMapper.writeValueAsString(payload);

        // Set up HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", googleApiKey);
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


    /**
     * Process route request and return route response
     * Main responsibilities:
     * 1. Call Google Routes API to get routing information
     * 2. Parse response and build RouteResponse object
     * 3. Process detailed route information including legs and steps
     *
     * @param request RouteRequest object containing origin, destination etc.
     * @return RouteResponse object containing complete routing information
     * @throws Exception when unable to retrieve route information
     */
    public RouteResponse getRoutes(RouteRequest request) throws Exception {
        JsonNode computeRoutesResponse = getResponse(request);
        if (computeRoutesResponse == null || !computeRoutesResponse.has("routes")) {
            throw new Exception("Unable to retrieve routes from Google Routes API.");
        }

        List<RouteResponse.RouteDetail> routeDetails = new ArrayList<>();
        for (JsonNode routeNode : computeRoutesResponse.get("routes")) {
            RouteResponse.RouteDetail routeDetail = createRouteDetail(routeNode);
            List<RouteResponse.LegDetail> legs = new ArrayList<>();

            if (routeNode.has("legs") && !routeNode.get("legs").isNull()) {
                for (JsonNode legNode : routeNode.get("legs")) {
                    RouteResponse.LegDetail legDetail = createLegDetail(legNode, request);
                    if (legNode.has("steps") && !legNode.get("steps").isNull()) {
                        legDetail.setSteps(processSteps(legNode));
                    }
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

    /**
     * Create route detail object from route node
     * Extract and set basic route information including:
     * - Summary
     * - Distance
     * - Duration
     * - Polyline encoding
     *
     * @param routeNode JsonNode containing route information
     * @return RouteResponse.RouteDetail object
     */
    private RouteResponse.RouteDetail createRouteDetail(JsonNode routeNode) {
        String summary = routeNode.has("summary") && !routeNode.get("summary").isNull()
                ? routeNode.get("summary").asText() : "No Summary";
        String distanceMeters = routeNode.has("distanceMeters") && !routeNode.get("distanceMeters").isNull()
                ? routeNode.get("distanceMeters").asText() : "Unknown";
        String duration = routeNode.has("duration") && !routeNode.get("duration").isNull()
                ? routeNode.get("duration").asText() : "Unknown";
        String polyline = "No Polyline";

        if (routeNode.has("polyline") && !routeNode.get("polyline").isNull()
                && routeNode.get("polyline").has("encodedPolyline")
                && !routeNode.get("polyline").get("encodedPolyline").isNull()) {
            polyline = routeNode.get("polyline").get("encodedPolyline").asText();
        }

        RouteResponse.RouteDetail routeDetail = new RouteResponse.RouteDetail();
        routeDetail.setSummary(summary);
        routeDetail.setDistanceMeters(distanceMeters);
        routeDetail.setDuration(duration);
        routeDetail.setPolyline(polyline);
        return routeDetail;
    }

    /**
     * Create details for single leg
     * Set basic leg properties including:
     * - Start location
     * - End location
     * - Travel mode
     * - Distance
     * - Duration
     *
     * @param legNode JsonNode containing leg information
     * @param request Original route request object
     * @return RouteResponse.LegDetail object
     */
    private RouteResponse.LegDetail createLegDetail(JsonNode legNode, RouteRequest request) {
        RouteResponse.LegDetail legDetail = new RouteResponse.LegDetail();

        legDetail.setStartLocation(legNode.has("startLocation") && !legNode.get("startLocation").isNull()
                ? legNode.get("startLocation").toString() : "Unknown Start Location");
        legDetail.setEndLocation(legNode.has("endLocation") && !legNode.get("endLocation").isNull()
                ? legNode.get("endLocation").toString() : "Unknown End Location");

        legDetail.setTravelMode("TRANSIT".equalsIgnoreCase(request.getTravelMode())
                ? "TRANSIT" : request.getTravelMode());

        legDetail.setDistance(legNode.has("distanceMeters") && !legNode.get("distanceMeters").isNull()
                ? legNode.get("distanceMeters").asText() : "Unknown Distance");
        legDetail.setDuration(legNode.has("duration") && !legNode.get("duration").isNull()
                ? legNode.get("duration").asText() : "Unknown Duration");

        return legDetail;
    }

    /**
     * Process all steps in a leg
     * Main responsibilities:
     * 1. Count number of transit steps
     * 2. Choose processing strategy based on transit count
     * 3. Apply different processing for high-frequency transfers vs normal cases
     *
     * @param legNode JsonNode containing steps information
     * @return List<RouteResponse.StepDetail> processed list of steps
     */
    private List<RouteResponse.StepDetail>  processSteps(JsonNode legNode) {
        List<RouteResponse.StepDetail> steps = new ArrayList<>();
        List<RouteResponse.StepDetail> transitSteps = new ArrayList<>();
        int transitCount = 0;

        // First pass: count transit steps and collect info
        for (JsonNode stepNode : legNode.get("steps")) {
            if (stepNode.has("travelMode") &&
                    "TRANSIT".equalsIgnoreCase(stepNode.get("travelMode").asText())) {
                transitCount++;
                if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()) {
                    RouteResponse.StepDetail stepDetail = setStepDetail(stepNode);
                    stepDetail.setTransitDetails(parseTransitDetails(stepNode.get("transitDetails")));
                    transitSteps.add(stepDetail);
                }
            }
        }

        // Choose processing strategy based on transit count
        if (transitCount > 5) {
            return processHighTransitSteps(legNode, transitSteps);
        } else {
            return processNormalSteps(legNode);
        }
    }

    /**
     * Process steps for high-frequency transfer cases (when a leg contains 5 or more transfers)
     * Optimization strategy:
     * 1. Sort transit steps by transfer time
     * 2. Keep 2-3 steps with the shortest transfer times
     * 3. Retain all walking steps and selected transit steps
     *
     * @param legNode JsonNode containing steps information
     * @param transitSteps List of collected transit steps
     * @return List<RouteResponse.StepDetail> optimized list of steps
     */
    private List<RouteResponse.StepDetail> processHighTransitSteps(JsonNode legNode,
                                                                   List<RouteResponse.StepDetail> transitSteps) {
        List<RouteResponse.StepDetail> steps = new ArrayList<>();

        // Sort by transfer time
        transitSteps.sort((s1, s2) -> {
            long time1 = getTransferTime(s1);
            long time2 = getTransferTime(s2);
            return Long.compare(time1, time2);
        });

        // Keep 2-3 steps with the shortest transfer times
        List<RouteResponse.StepDetail> keptSteps = transitSteps.subList(
                0,
                Math.min(3, transitSteps.size())
        );

        // Re-traverse steps, keeping non-transit steps and selected transit steps
        for (JsonNode stepNode : legNode.get("steps")) {
            String stepTravelMode = stepNode.has("travelMode") ?
                    stepNode.get("travelMode").asText() : "WALK";

            if (!"TRANSIT".equalsIgnoreCase(stepTravelMode)) {
                steps.add(setStepDetail(stepNode));
            } else if (stepNode.has("transitDetails")) {
                RouteResponse.StepDetail stepDetail = setStepDetail(stepNode);
                RouteResponse.StepDetail.TransitDetails td = parseTransitDetails(stepNode.get("transitDetails"));
                stepDetail.setTransitDetails(td);

                if (isStepInKeptList(td, keptSteps)) {
                    steps.add(stepDetail);
                }
            }
        }

        return steps;
    }

    /**
     * Get transfer time from transit details
     * Returns waitTimeSeconds directly from transit details if available
     * Returns maximum value if information cannot be retrieved
     *
     * @param step Step detail object containing transit details
     * @return long Transfer/wait time in seconds
     */
    private long getTransferTime(RouteResponse.StepDetail step) {
        if (step.getTransitDetails() != null) {
            return step.getTransitDetails().getWaitTimeSeconds();
        }
        return Long.MAX_VALUE;
    }


    /**
     * Check if given transit step is in the kept list
     * Matches by comparing departure stop locations
     *
     * @param td Transit details object
     * @param keptSteps List of steps to keep
     * @return boolean True if step is in kept list
     */
    private boolean isStepInKeptList(RouteResponse.StepDetail.TransitDetails td,
                                     List<RouteResponse.StepDetail> keptSteps) {
        return keptSteps.stream().anyMatch(kept ->
                kept.getTransitDetails().getStopDetails().getDepartureStop().getLocation()
                        .equals(td.getStopDetails().getDepartureStop().getLocation())
        );
    }

    /**
     * Process steps for normal cases (less than 5 transfers)
     * Main processing:
     * 1. Calculate walking duration
     * 2. Handle waiting times
     * 3. Process special cases (e.g., waiting time > 20 minutes)
     *
     * @param legNode JsonNode containing steps information
     * @return List<RouteResponse.StepDetail> processed list of steps
     */
    private List<RouteResponse.StepDetail> processNormalSteps(JsonNode legNode) {
        List<RouteResponse.StepDetail> steps = new ArrayList<>();
        String previousTravelMode = "WALK";
        long previousWalkDuration = 0;
        Instant curStepArrivalTime = Instant.now();
        Instant previousStepArrivalTime = Instant.now();

        for (int i = 0; i < legNode.get("steps").size(); i++) {
            JsonNode stepNode = legNode.get("steps").get(i);
            RouteResponse.StepDetail stepDetail = setStepDetail(stepNode);
            String stepTravelMode = stepNode.has("travelMode")
                    ? stepNode.get("travelMode").asText()
                    : "WALK";

            // 累计步行时间
            if ("WALK".equalsIgnoreCase(previousTravelMode) &&
                    "WALK".equalsIgnoreCase(stepTravelMode)) {
                previousWalkDuration += stepDetail.getDuration();
            }

            if ("TRANSIT".equalsIgnoreCase(stepTravelMode)) {
                if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()) {
                    JsonNode transitDetails = stepNode.get("transitDetails");
                    boolean isCrossSea = TimingUtils.measureExecutionTime("检查是否为跨海路线耗时",
                            () -> CrossSeaRouteChecker.isCrossSeaRoute(transitDetails));

                    boolean isCongested = TimingUtils.measureExecutionTime("检查出发站点是否为拥堵站点耗时",
                            () -> CongestionStationChecker.isCongested(transitDetails));

                    // 检查是否为打车热点
                    boolean isTaxiHotspot = TimingUtils.measureExecutionTime("检查是否为打车热点耗时",
                            () -> {
                                try {
                                    return TaxiHotSpotChecker.isHotspot(
                                            transitDetails.get("departure_stop"));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                    RouteResponse.StepDetail.TransitDetails td = parseTransitDetails(transitDetails);
                    stepDetail.setTransitDetails(td);

                    // 更新到达时间
                    if (td.getStopDetails() != null && td.getStopDetails().getArrivalTime() != null) {
                        curStepArrivalTime = Instant.parse(td.getStopDetails().getArrivalTime());
                    }

                    // 检查是否需要替换路段
                    if ("WALK".equalsIgnoreCase(previousTravelMode) &&
                            td.getStopDetails() != null &&
                            td.getStopDetails().getDepartureTime() != null) {

                        try {
                            Instant departureTime = Instant.parse(td.getStopDetails().getDepartureTime());
                            Instant walkToStationTime = previousStepArrivalTime.plusSeconds(previousWalkDuration);
                            long waitTimeSeconds = Duration.between(walkToStationTime, departureTime).getSeconds();
                            td.setWaitTimeSeconds(waitTimeSeconds);
                            stepDetail.setTransitDetails(td);

                            // 修改判断条件：当等待时间过长或站点拥堵时都考虑替换路段
                            // 但跨海路线除外
                            if ((waitTimeSeconds > MAX_WAIT_TIME_SECONDS || isCongested || isTaxiHotspot) && !isCrossSea) {
                                // 找到当前公交段的终点站
                                int j = i;
                                RouteResponse.StepDetail.TransitDetails.StopDetails.Stop startStop =
                                        td.getStopDetails().getDepartureStop();
                                RouteResponse.StepDetail.TransitDetails.StopDetails.Stop endStop = null;

                                // 查找终点站（不修改原始的 i）
                                while (j < legNode.get("steps").size()) {
                                    JsonNode currentNode = legNode.get("steps").get(j);
                                    String currentMode = currentNode.has("travelMode")
                                            ? currentNode.get("travelMode").asText()
                                            : "WALK";

                                    if ("TRANSIT".equalsIgnoreCase(currentMode)) {
                                        JsonNode currentTransitDetails = currentNode.get("transitDetails");
                                        if (currentTransitDetails != null && !currentTransitDetails.isNull()) {
                                            RouteResponse.StepDetail.TransitDetails currentTd =
                                                    parseTransitDetails(currentTransitDetails);
                                            if (currentTd.getStopDetails() != null) {
                                                endStop = currentTd.getStopDetails().getArrivalStop();
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                    j++;
                                }

                                // 如果找到了完整的起终点，进行替换路段操作
                                if (startStop != null && endStop != null) {
                                    List<RouteResponse.StepDetail> replacementSteps =
                                            changeStep(startStop, endStop);
                                    if (!replacementSteps.isEmpty()) {
                                        steps.addAll(replacementSteps);
                                        // 跳过被替换的路段
                                        i = j - 1;
                                        continue;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.warning("处理等待时间时发生错误");
                        }
                    }
                }
            }

            previousStepArrivalTime = curStepArrivalTime;
            previousTravelMode = stepTravelMode;
            steps.add(stepDetail);

            // 重置步行时间
            if (!("WALK".equalsIgnoreCase(previousTravelMode) &&
                    "WALK".equalsIgnoreCase(stepTravelMode))) {
                previousWalkDuration = 0;
            }
        }

        return steps;
    }


    // 替换公交路径为OTP返回的驾驶路径
    public List<RouteResponse.StepDetail> changeStep(RouteResponse.StepDetail.TransitDetails.StopDetails.Stop startStop,
                                                     RouteResponse.StepDetail.TransitDetails.StopDetails.Stop endStop) throws Exception {
        return TimingUtils.measureExecutionTime("路径替换操作",
                () -> {
                    try {
                        return otpService.getDrivingRoute(startStop, endStop);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }


    /**
     * Calculate time difference between two time points
     * Parses time strings to Instant objects and calculates difference
     * Returns maximum value if parsing fails
     *
     * @param departureTime Departure time string
     * @param arrivalTime Arrival time string
     * @return long Time difference in seconds
     */
    private long calculateTransferTime(String departureTime, String arrivalTime) {
        try {
            Instant departure = Instant.parse(departureTime);
            Instant arrival = Instant.parse(arrivalTime);
            return Duration.between(departure, arrival).getSeconds();
        } catch (Exception e) {
            logger.warning("Error calculating transfer time: " + e.getMessage());
            return Long.MAX_VALUE;
        }
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
     * 在此方法内也对 arrivalTime 做了读取，但只是存储和返回，不在此处做等待时间大于 180 秒的替换逻辑
     * （可在创建 stepDetail 时进行更灵活的判断）
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
            // 把 arrivalTime 原样存储下来，后续在创建 StepDetail 时判断
            if (stopDetailsNode.has("arrivalTime") && !stopDetailsNode.get("arrivalTime").isNull()) {
                stopDetails.setArrivalTime(stopDetailsNode.get("arrivalTime").asText());
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

            if (stopDetailsNode.has("departureTime") && !stopDetailsNode.get("departureTime").isNull()) {
                stopDetails.setDepartureTime(stopDetailsNode.get("departureTime").asText());
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
                    RouteResponse.StepDetail.TransitDetails.TransitLine.Agency agency =
                            new RouteResponse.StepDetail.TransitDetails.TransitLine.Agency();
                    agency.setName(agencyNode.has("name") ? agencyNode.get("name").asText() : null);
                    agency.setPhoneNumber(agencyNode.has("phoneNumber") ? agencyNode.get("phoneNumber").asText() : null);
                    agency.setUri(agencyNode.has("uri") ? agencyNode.get("uri").asText() : null);
                    agencies.add(agency);
                }
                transitLine.setAgencies(agencies);
            }

            transitLine.setName(transitLineNode.has("name") ? transitLineNode.get("name").asText() : null);
            transitLine.setColor(transitLineNode.has("color") ? transitLineNode.get("color").asText() : null);
            transitLine.setNameShort(transitLineNode.has("nameShort") ? transitLineNode.get("nameShort").asText() : null);
            transitLine.setTextColor(transitLineNode.has("textColor") ? transitLineNode.get("textColor").asText() : null);

            // Vehicle
            if (transitLineNode.has("vehicle") && !transitLineNode.get("vehicle").isNull()) {
                JsonNode vehicleNode = transitLineNode.get("vehicle");
                RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle vehicle =
                        new RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle();

                if (vehicleNode.has("name") && !vehicleNode.get("name").isNull()) {
                    RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle.Name name =
                            new RouteResponse.StepDetail.TransitDetails.TransitLine.Vehicle.Name();
                    name.setText(vehicleNode.get("name").has("text")
                            ? vehicleNode.get("name").get("text").asText()
                            : null);
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

//        // Set Compute Alternative Routes
//        payload.setComputeAlternativeRoutes(true);

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
