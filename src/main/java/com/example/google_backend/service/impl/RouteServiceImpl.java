package com.example.google_backend.service.impl;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.RouteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class RouteServiceImpl implements RouteService {

    @Value("${google.api.key}")
    private String googleApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = Logger.getLogger(RouteService.class.getName());

    /**
     * Retrieves routes based on the provided request.
     *
     * @param request The route calculation request.
     * @return The route response containing route details.
     * @throws Exception if an error occurs during API calls.
     */
    public RouteResponse getRoutes(RouteRequest request) throws Exception {
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

        if (computeRoutesResponse == null || !computeRoutesResponse.has("routes")) {
            throw new Exception("Unable to retrieve routes from Google Routes API.");
        }

        // **新增日志记录：记录完整的 API 响应**
//        logger.info("Compute Routes Response: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(computeRoutesResponse));

        // Parse route information
        List<RouteResponse.RouteDetail> routeDetails = new ArrayList<>();
        for (JsonNode routeNode : computeRoutesResponse.get("routes")) {
            String summary = routeNode.has("summary") && !routeNode.get("summary").isNull() ? routeNode.get("summary").asText() : "No Summary";
            String distanceMeters = routeNode.has("distanceMeters") && !routeNode.get("distanceMeters").isNull() ? routeNode.get("distanceMeters").asText() : "Unknown";
            String duration = routeNode.has("duration") && !routeNode.get("duration").isNull() ? routeNode.get("duration").asText() : "Unknown";
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

            // Initialize legs list
            List<RouteResponse.LegDetail> legs = new ArrayList<>();

            // Check if stepsOverview exists to handle multi-modal segments
            if (routeNode.has("stepsOverview") && !routeNode.get("stepsOverview").isNull()
                    && routeNode.get("stepsOverview").has("multiModalSegments")) {
                JsonNode multiModalSegments = routeNode.get("stepsOverview").get("multiModalSegments");
                for (JsonNode segment : multiModalSegments) {
                    String travelMode = segment.has("travelMode") && !segment.get("travelMode").isNull() ? segment.get("travelMode").asText() : "WALK";
                    int stepStartIndex = segment.has("stepStartIndex") ? segment.get("stepStartIndex").asInt() : 0;
                    int stepEndIndex = segment.has("stepEndIndex") ? segment.get("stepEndIndex").asInt() : 0;

                    RouteResponse.LegDetail legDetail = new RouteResponse.LegDetail();
                    legDetail.setTravelMode(travelMode);

                    // Assume startLocation and endLocation based on first and last steps in the segment
                    JsonNode steps = routeNode.get("steps");
                    if (steps != null && steps.isArray()) {
                        if (steps.size() > stepStartIndex) {
                            JsonNode startStep = steps.get(stepStartIndex);
                            if (startStep.has("startLocation") && !startStep.get("startLocation").isNull()) {
                                legDetail.setStartLocation(startStep.get("startLocation").toString());
                            } else {
                                legDetail.setStartLocation("Unknown Start Location");
                            }
                        }

                        if (steps.size() > stepEndIndex) {
                            JsonNode endStep = steps.get(stepEndIndex);
                            if (endStep.has("endLocation") && !endStep.get("endLocation").isNull()) {
                                legDetail.setEndLocation(endStep.get("endLocation").toString());
                            } else {
                                legDetail.setEndLocation("Unknown End Location");
                            }
                        }
                    } else {
                        legDetail.setStartLocation("Unknown Start Location");
                        legDetail.setEndLocation("Unknown End Location");
                    }

                    // Extract distance and duration for the segment
                    // Sum up distance and duration from individual steps
                    long totalDistance = 0;
                    long totalDuration = 0;
                    List<RouteResponse.StepDetail> stepDetails = new ArrayList<>();

                    for (int i = stepStartIndex; i <= stepEndIndex && i < routeNode.get("steps").size(); i++) {
                        JsonNode stepNode = routeNode.get("steps").get(i);
                        RouteResponse.StepDetail stepDetail = new RouteResponse.StepDetail();

                        // Common step fields
                        stepDetail.setInstruction(stepNode.has("navigationInstruction") && stepNode.get("navigationInstruction").has("instructions")
                                ? stepNode.get("navigationInstruction").get("instructions").asText()
                                : "No Instruction");
                        stepDetail.setDistance(stepNode.has("distanceMeters") ? stepNode.get("distanceMeters").asLong() : 0L);
                        stepDetail.setDuration(stepNode.has("staticDuration") ? parseDuration(stepNode.get("staticDuration").asText()) : 0L);
                        stepDetail.setPolyline(stepNode.has("polyline") && !stepNode.get("polyline").isNull()
                                && stepNode.get("polyline").has("encodedPolyline")
                                ? stepNode.get("polyline").get("encodedPolyline").asText()
                                : "No Polyline");

                        // Additional fields based on travelMode
                        if ("TRANSIT".equalsIgnoreCase(travelMode)) {
                            if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()) {
                                // Extract transit-specific details
                                JsonNode transitDetails = stepNode.get("transitDetails");
                                stepDetail.setTransitDetails(parseTransitDetails(transitDetails));
                            }
                        }

                        stepDetails.add(stepDetail);

                        // Accumulate distance and duration
                        totalDistance += stepDetail.getDistance();
                        totalDuration += stepDetail.getDuration();
                    }

                    legDetail.setSteps(stepDetails);
                    legDetail.setDistance(String.valueOf(totalDistance));
                    legDetail.setDuration(String.valueOf(totalDuration));

                    legs.add(legDetail);
                }
            } else {
                // Fallback to handling steps without multi-modal segments
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

                        // Extract distance and duration
                        legDetail.setDistance(legNode.has("distanceMeters") && !legNode.get("distanceMeters").isNull()
                                ? legNode.get("distanceMeters").asText()
                                : "Unknown Distance");
                        legDetail.setDuration(legNode.has("duration") && !legNode.get("duration").isNull()
                                ? legNode.get("duration").asText()
                                : "Unknown Duration");

                        // Handle steps within the leg
                        if (legNode.has("steps") && !legNode.get("steps").isNull()) {
                            List<RouteResponse.StepDetail> steps = new ArrayList<>();
                            for (JsonNode stepNode : legNode.get("steps")) {
                                RouteResponse.StepDetail stepDetail = new RouteResponse.StepDetail();

                                // Common step fields
                                stepDetail.setInstruction(stepNode.has("navigationInstruction") && stepNode.get("navigationInstruction").has("instructions")
                                        ? stepNode.get("navigationInstruction").get("instructions").asText()
                                        : "No Instruction");
                                stepDetail.setDistance(stepNode.has("distanceMeters") ? stepNode.get("distanceMeters").asLong() : 0L);
                                stepDetail.setDuration(stepNode.has("staticDuration") ? parseDuration(stepNode.get("staticDuration").asText()) : 0L);
                                stepDetail.setPolyline(stepNode.has("polyline") && !stepNode.get("polyline").isNull()
                                        && stepNode.get("polyline").has("encodedPolyline")
                                        ? stepNode.get("polyline").get("encodedPolyline").asText()
                                        : "No Polyline");

                                // Additional fields based on travelMode
                                String stepTravelMode = stepNode.has("travelMode") ? stepNode.get("travelMode").asText() : "WALK";
                                if ("TRANSIT".equalsIgnoreCase(stepTravelMode)) {
                                    if (stepNode.has("transitDetails") && !stepNode.get("transitDetails").isNull()) {
                                        // Extract transit-specific details
                                        JsonNode transitDetails = stepNode.get("transitDetails");
                                        stepDetail.setTransitDetails(parseTransitDetails(transitDetails));
                                    }
                                }

                                steps.add(stepDetail);
                            }
                            legDetail.setSteps(steps);
                        } else {
                            legDetail.setSteps(new ArrayList<>()); // 设置为空列表
                        }

                        legs.add(legDetail);
                    }
                }
            }

            routeDetail.setLegs(legs);
            routeDetails.add(routeDetail);
        }

        // **新增日志记录：记录获取所有路线所需的时间和获取的路线数量**
        long totalTimeElapsed = Duration.between(startTime, endTime).toMillis(); // 计算总时间
        logger.info("Total time taken to fetch all routes: " + totalTimeElapsed + " ms");
        logger.info("Number of routes fetched: " + routeDetails.size());

        RouteResponse response = new RouteResponse();
        response.setRoutes(routeDetails);

        return response;
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

    // Define the payload structure as per Google Routes API
    private static class RouteRequestPayload {
        private Origin origin;
        private Destination destination;
        private String travelMode;
        private TransitPreferences transitPreferences;
        private boolean computeAlternativeRoutes;

        // Getters and Setters

        public Origin getOrigin() {
            return origin;
        }

        public void setOrigin(Origin origin) {
            this.origin = origin;
        }

        public Destination getDestination() {
            return destination;
        }

        public void setDestination(Destination destination) {
            this.destination = destination;
        }

        public String getTravelMode() {
            return travelMode;
        }

        public void setTravelMode(String travelMode) {
            this.travelMode = travelMode;
        }

        public TransitPreferences getTransitPreferences() {
            return transitPreferences;
        }

        public void setTransitPreferences(TransitPreferences transitPreferences) {
            this.transitPreferences = transitPreferences;
        }

        public boolean isComputeAlternativeRoutes() {
            return computeAlternativeRoutes;
        }

        public void setComputeAlternativeRoutes(boolean computeAlternativeRoutes) {
            this.computeAlternativeRoutes = computeAlternativeRoutes;
        }

        // Origin Class
        private static class Origin {
            private Location location;

            public Location getLocation() {
                return location;
            }

            public void setLocation(Location location) {
                this.location = location;
            }
        }

        // Destination Class
        private static class Destination {
            private Location location;

            public Location getLocation() {
                return location;
            }

            public void setLocation(Location location) {
                this.location = location;
            }
        }

        // Location Class
        private static class Location {
            private LatLng latLng;

            public Location() {}

            public Location(LatLng latLng) {
                this.latLng = latLng;
            }

            public LatLng getLatLng() {
                return latLng;
            }

            public void setLatLng(LatLng latLng) {
                this.latLng = latLng;
            }
        }

        // LatLng Class
        private static class LatLng {
            private double latitude;
            private double longitude;

            public LatLng() {}

            public LatLng(double latitude, double longitude) {
                this.latitude = latitude;
                this.longitude = longitude;
            }

            public double getLatitude() {
                return latitude;
            }

            public void setLatitude(double latitude) {
                this.latitude = latitude;
            }

            public double getLongitude() {
                return longitude;
            }

            public void setLongitude(double longitude) {
                this.longitude = longitude;
            }
        }

        // TransitPreferences Class
        private static class TransitPreferences {
            private List<String> allowedTravelModes;

            public List<String> getAllowedTravelModes() {
                return allowedTravelModes;
            }

            public void setAllowedTravelModes(List<String> allowedTravelModes) {
                this.allowedTravelModes = allowedTravelModes;
            }
        }
    }
}
