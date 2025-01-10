//package com.example.google_backend.controller;
//
//import com.example.google_backend.model.RouteRequest;
//import com.fasterxml.jackson.databind.JsonNode;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
///**
// * Controller to handle route-related endpoints.
// */
//@RestController
//@RequestMapping("/api/routes")
//@CrossOrigin(origins = "http://localhost:3000") // Adjust the origin as needed
//public class RouteController2 {
//
//    @Autowired
//    private RouteService2 routeService;
//
//    /**
//     * Endpoint to retrieve routes based on the provided request.
//     *
//     * @param routeRequest The route calculation request payload.
//     * @return The raw JSON response from Google Routes API.
//     */
//    @PostMapping
//    public ResponseEntity<JsonNode> getRoutes(@RequestBody RouteRequest routeRequest) {
//        try {
//            JsonNode routesResponse = routeService.getRoutes(routeRequest);
//            return ResponseEntity.ok(routesResponse);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(null);
//        }
//    }
//}