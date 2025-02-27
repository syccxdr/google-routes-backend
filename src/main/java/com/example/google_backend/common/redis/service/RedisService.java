package com.example.google_backend.common.redis.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 */
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisService {
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }

    // ========== Geospatial 相关操作 ==========

    /**
     * 添加地理位置信息
     *
     * @param key Redis键
     * @param longitude 经度
     * @param latitude 纬度
     * @param member 成员名称
     * @return 添加的数量
     */
    public Long geoAdd(String key, double longitude, double latitude, String member) {
        Point point = new Point(longitude, latitude);
        return redisTemplate.opsForGeo().add(key, point, member);
    }

    /**
     * 批量添加地理位置信息
     *
     * @param key Redis键
     * @param memberCoordinateMap 成员坐标映射
     * @return 添加的数量
     */
    public Long geoAdd(String key, Map<String, Point> memberCoordinateMap) {
        Map<Object, Point> map = (Map) memberCoordinateMap;
        return redisTemplate.opsForGeo().add(key, map);
    }

    /**
     * 获取地理位置信息
     *
     * @param key Redis键
     * @param member 成员名称
     * @return 坐标点
     */
    public Point geoPos(String key, String member) {
        List<Point> list = redisTemplate.opsForGeo().position(key, member);
        return (list != null && !list.isEmpty()) ? list.getFirst() : null;
    }

    /**
     * 获取多个地理位置信息
     *
     * @param key Redis键
     * @param members 成员名称集合
     * @return 坐标点列表
     */
    public List<Point> geoPos(String key, String... members) {
        return redisTemplate.opsForGeo().position(key, members);
    }

    /**
     * 计算两点之间的距离
     *
     * @param key Redis键
     * @param member1 成员1
     * @param member2 成员2
     * @param metric 距离单位
     * @return 距离
     */
    public Distance geoDist(String key, String member1, String member2, Metrics metric) {
        return redisTemplate.opsForGeo().distance(key, member1, member2, metric);
    }


    /**
     * 获取指定范围内的地理位置
     *
     * @param key Redis键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param metric 距离单位
     * @return 地理位置结果集
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> geoRadius(
            String key, double longitude, double latitude, double radius, Metrics metric) {

        // 创建圆形区域
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radius, metric);
        Circle circle = new Circle(center, distance);

        // 执行查询
        return redisTemplate.opsForGeo().radius(key, circle);
    }



    /**
     * 获取指定范围内的地理位置（带距离信息）
     *
     * @param key Redis键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径
     * @param metric 距离单位
     * @return 地理位置结果集
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> geoRadiusWithDistance(
            String key, double longitude, double latitude, double radius, Metrics metric) {

        // 创建圆形区域
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radius, metric);

        // 设置查询参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance();

        // 执行查询
        return redisTemplate.opsForGeo().radius(key, center, distance, args);
    }

    /**
     * 获取指定范围内的地理位置（带距离信息和限制数量）
     *
     * @param key Redis键
     * @param longitude 中心点经度
     * @param latitude 中心点纬度
     * @param radius 半径（米）
     * @param metrics 距离单位
     * @param limit 返回结果数量限制
     * @param isAsc 是否按距离升序排序
     * @return 地理位置结果集
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> geoRadiusWithDistanceAndLimit(
            String key, double longitude, double latitude, double radius, Metrics metrics, long limit, boolean isAsc) {

        // 创建圆形区域
        Point center = new Point(longitude, latitude);
        Distance distance = new Distance(radius, metrics);

        // 设置查询参数
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance();

        // 设置排序和限制
        if (isAsc) {
            args.sortAscending();
        } else {
            args.sortDescending();
        }

        if (limit > 0) {
            args.limit(limit);
        }

        // 执行查询
        return redisTemplate.opsForGeo().radius(key, center, distance, args);
    }


    /**
     * 获取指定成员附近的地理位置
     *
     * @param key Redis键
     * @param member 成员名称
     * @param radius 半径
     * @param metric a距离单位
     * @return 地理位置结果集
     */
    public GeoResults<RedisGeoCommands.GeoLocation<String>> geoRadiusByMember(
            String key, String member, double radius, Metrics metric) {

        // 设置距离
        Distance distance = new Distance(radius, metric);

        // 执行查询
        return redisTemplate.opsForGeo().radius(key, member, distance);
    }


    /**
     * 获取地理位置的GeoHash
     *
     * @param key Redis键
     * @param members 成员名称集合
     * @return GeoHash列表
     */
    public List<String> geoHash(String key, String... members) {
        return redisTemplate.opsForGeo().hash(key, members);
    }
}