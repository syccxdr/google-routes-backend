package com.example.google_backend.service.impl;

import com.example.google_backend.common.redis.service.RedisService;
import com.example.google_backend.service.AsyncCacheService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AsyncCacheServiceImpl implements AsyncCacheService {

    private static final Logger logger = Logger.getLogger(AsyncCacheServiceImpl.class.getName());

    @Resource
    private RedisService redisService;

    /**
     * 异步缓存路由响应到Redis
     *
     * @param cacheKey Redis缓存键
     * @param response 要缓存的路由响应对象
     * @param expireTime 缓存过期时间（分钟）
     */
    @Async
    public void cacheRouteAsync(String cacheKey, Object response, int expireTime) {
        try {
            logger.info("开始异步缓存操作，缓存键: {}" + cacheKey);
            redisService.setCacheObject(cacheKey, response, (long) expireTime, TimeUnit.MINUTES);
            logger.info("路径已异步缓存 - 有效期:" + expireTime + "  分钟 ");
        } catch (Exception e) {
            logger.warning("异步缓存操作失败" + cacheKey + "错误: " + e.getMessage());
        }
    }

    /**
     * 异步删除缓存
     *
     * @param cacheKey Redis缓存键
     */
    @Async
    public void deleteCacheAsync(String cacheKey) {
        try {
            logger.fine("开始异步删除缓存，缓存键: " + cacheKey);
            redisService.deleteObject(cacheKey);
            logger.info("缓存已异步删除 - 缓存键: " + cacheKey);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "异步删除缓存失败 - 缓存键: " + cacheKey + " - 错误: " + e.getMessage(), e);
        }
    }

    /**
     * 异步批量缓存多个对象
     *
     * @param keyPrefix 缓存键前缀
     * @param responses 要缓存的对象列表
     * @param expireTime 缓存过期时间（分钟）
     */
    @Async
    public void batchCacheAsync(String keyPrefix, List<Object> responses, int expireTime) {
        try {
            logger.fine("开始异步批量缓存操作，前缀: " + keyPrefix + ", 对象数量: " + responses.size());

            for (int i = 0; i < responses.size(); i++) {
                String cacheKey = keyPrefix + ":" + i;
                redisService.setCacheObject(cacheKey, responses.get(i), (long) expireTime, TimeUnit.MINUTES);
            }

            logger.info("批量缓存完成 - 共缓存 " + responses.size() + " 个对象 - 前缀: " + keyPrefix);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "异步批量缓存操作失败 - 前缀: " + keyPrefix + " - 错误: " + e.getMessage(), e);
        }
    }
}


