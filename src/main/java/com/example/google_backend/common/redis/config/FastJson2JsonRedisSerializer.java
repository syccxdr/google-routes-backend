package com.example.google_backend.common.redis.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis使用FastJson2序列化
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T>
{
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // 定义需要支持自动类型的安全包名白名单
    private static final String[] AUTO_TYPE_WHITELIST = {
            "com.example.google_backend.model.",
            "org.springframework"
            // 根据需要添加其他安全类
    };

    // 创建自动类型过滤器
    static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(AUTO_TYPE_WHITELIST);

    private Class<T> clazz;

    public FastJson2JsonRedisSerializer(Class<T> clazz)
    {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException
    {
        if (t == null)
        {
            return new byte[0];
        }
        try {
            return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
        } catch (Exception ex) {
            throw new SerializationException("Error serializing object using FastJson2", ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length <= 0)
        {
            return null;
        }
        try {
            String str = new String(bytes, DEFAULT_CHARSET);
            return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
        } catch (Exception ex) {
            throw new SerializationException("Error deserializing object using FastJson2", ex);
        }
    }
}