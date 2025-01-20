package com.example.google_backend.controller;

import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.RouteService;
import com.example.google_backend.utils.TimingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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
            // 记录路线计算服务的耗时
            RouteResponse response = TimingUtils.measureExecutionTime("路线计算服务耗时",
                    () -> {
                        try {
                            return routeService.getRoutes(routeRequest);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the error and return an appropriate response
            return ResponseEntity.status(500).body("Error calculating routes: " + e.getMessage());
        }
    }

    @PostMapping("/sorted")
    public ResponseEntity<?> getSortedRoutes(
            @RequestBody RouteRequest routeRequest,
            @RequestParam(value = "sort", defaultValue = "shortestDuration") String sortType) {
        try {
            // Step 1: 获取所有路线
            RouteResponse response = routeService.getRoutes(routeRequest);
            List<RouteResponse.RouteDetail> allRoutes = response.getRoutes();

            // Step 2: 按指定方式排序
            List<RouteResponse.RouteDetail> sortedRoutes = routeService.sortRoutes(allRoutes, sortType);

            // 返回排序后的路线
            return ResponseEntity.ok(sortedRoutes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid sort type: " + sortType);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sorting routes: " + e.getMessage());
        }
    }
}