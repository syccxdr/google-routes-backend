package com.example.google_backend.service;

import java.util.List;

public interface AsyncCacheService {
    /**
     * 异步缓存路由响应到Redis
     *
     * @param cacheKey Redis缓存键
     * @param response 要缓存的路由响应对象
     * @param expireTime 缓存过期时间（分钟）
     */
    void cacheRouteAsync(String cacheKey, Object response, int expireTime);

    /**
     * 异步删除缓存
     *
     * @param cacheKey Redis缓存键
     */
    void deleteCacheAsync(String cacheKey);

    /**
     * 异步批量缓存多个对象
     *
     * @param keyPrefix 缓存键前缀
     * @param responses 要缓存的对象列表
     * @param expireTime 缓存过期时间（分钟）
     */
    void batchCacheAsync(String keyPrefix, List<Object> responses, int expireTime);

}
