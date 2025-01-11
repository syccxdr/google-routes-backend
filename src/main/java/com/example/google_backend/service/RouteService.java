package com.example.google_backend.service;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
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

/**
 * Service to handle route calculations by interacting with Google APIs.
 */

public interface RouteService {

    /**
     * Retrieves routes based on the provided request.
     *
     * @param request The route calculation request.
     * @return The route response containing route details.
     * @throws Exception if an error occurs during API calls.
     */
    RouteResponse getRoutes(RouteRequest request) throws Exception;

}