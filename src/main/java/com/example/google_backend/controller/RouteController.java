package com.example.google_backend.controller;

import com.example.google_backend.common.redis.service.RedisService;
import com.example.google_backend.model.RouteRequest;
import com.example.google_backend.model.RouteResponse;
import com.example.google_backend.service.AsyncCacheService;
import com.example.google_backend.service.RouteService;
import com.example.google_backend.service.impl.RouteServiceImpl;
import com.example.google_backend.utils.TimingUtils;
import com.example.google_backend.utils.generator.CacheKeyGenerator;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Controller to handle route calculation requests.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Resource
    private RouteService routeService;

    @Resource
    private RedisService redisService;

    @Resource
    private AsyncCacheService asyncCacheService;

    //路线缓存过期时间 (minute)
    private static final int ROUTE_CACHE_EXPIRE_TIME = 15;

    private final Logger logger = Logger.getLogger(RouteController.class.getName());


    /**
     * Endpoint to calculate routes based on the provided request.
     *
     * @param routeRequest The route calculation request containing origin, destination, travel mode, and transit modes.
     * @return A ResponseEntity containing the route response.
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateRoutes(@RequestBody RouteRequest routeRequest) {

        try {
            String cacheKey = CacheKeyGenerator.generateRouteKey(routeRequest);
            // Step 1: 检查缓存
            if(redisService.hasKey(cacheKey)){
                logger.info("路径缓存命中 - /calculate - 缓存键: " + cacheKey);
                return ResponseEntity.ok(redisService.getCacheObject(cacheKey));
            }else{
                logger.info("路径缓存未命中 - /calculate - 缓存键: {}" + cacheKey);
                // 缓存没查到，调Google API; 记录路线计算服务的耗时
                RouteResponse response = TimingUtils.measureExecutionTime("路线计算服务耗时",
                        () -> {
                            try {
                                return routeService.getRoutes(routeRequest);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                // 异步缓存路线到redis
                asyncCacheService.cacheRouteAsync(cacheKey, response, ROUTE_CACHE_EXPIRE_TIME);
                // 缓存保存日志
                logger.info("路径已缓存 - 有效期: " + ROUTE_CACHE_EXPIRE_TIME + " 分钟 - 缓存键: " + cacheKey);;
                return ResponseEntity.ok(response);

            }
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
            String cacheKey = CacheKeyGenerator.generateRouteKey(routeRequest);

            List<RouteResponse.RouteDetail> allRoutes;

            if(redisService.hasKey(cacheKey)){
                logger.info("路径缓存命中 - /sorted - 缓存键: " + cacheKey + " - 排序方式: " + sortType);
                RouteResponse cachedResponse = redisService.getCacheObject(cacheKey);
                allRoutes = cachedResponse.getRoutes();
            }else{
                logger.info("路径缓存未命中 - /sorted - 缓存键: " + cacheKey) ;
                // 注意这里直接走service层方法，不再调用calculateRoutes方法
                RouteResponse response = routeService.getRoutes(routeRequest);
                allRoutes = response.getRoutes();
                // 异步缓存路线到redis
                asyncCacheService.cacheRouteAsync(cacheKey, response, ROUTE_CACHE_EXPIRE_TIME);
                logger.info("路径已缓存 - 有效期: " + ROUTE_CACHE_EXPIRE_TIME + " 分钟 - 缓存键: " + cacheKey);
            }

            // Step 2: 按指定方式排序
            List<RouteResponse.RouteDetail> sortedRoutes = TimingUtils.measureExecutionTime(
                    "路径排序耗时",
                    () -> routeService.sortRoutes(allRoutes, sortType)
            );

            // 返回排序后的路线
            return ResponseEntity.ok(sortedRoutes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid sort type: " + sortType);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sorting routes: " + e.getMessage());
        }
    }
}