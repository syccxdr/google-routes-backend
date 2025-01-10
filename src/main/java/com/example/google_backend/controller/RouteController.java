package com.example.google_backend.controller;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to handle route calculation requests.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    /**
     * Endpoint to calculate routes based on the provided request.
     *
     * @param routeRequest The route calculation request containing origin, destination, travel mode, and transit modes.
     * @return A ResponseEntity containing the route response.
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateRoutes(@RequestBody RouteRequest routeRequest) {
        try {
            RouteResponse response = routeService.getRoutes(routeRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the error and return an appropriate response
            return ResponseEntity.status(500).body("Error calculating routes: " + e.getMessage());
        }
    }
}