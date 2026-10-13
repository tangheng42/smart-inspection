package com.zhixunyun.service;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 只缓存无关键字的区域和设备类型下拉选项，Key 按选项类型固定。
 * 写入时设置可配置 TTL；读取失败视为缓存未命中，写入或删除失败只记录日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptionCacheService {

    public static final String AREA_OPTIONS_KEY = "zhixunyun:area:options:all";
    public static final String EQUIPMENT_TYPE_OPTIONS_KEY =
            "zhixunyun:equipment-type:options:all";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zhixunyun.redis.enabled:false}")
    private boolean enabled;

    @Value("${zhixunyun.redis.option-ttl:30m}")
    private Duration optionTtl;

    public <T> List<T> getList(String key, Class<T> elementType) {
        if (!enabled) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            JavaType type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, elementType);
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            log.warn("读取 Redis 缓存失败，已回退数据库，key={}", key, ex);
            return null;
        }
    }

    public void put(String key, Object value) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    key, objectMapper.writeValueAsString(value), optionTtl);
        } catch (Exception ex) {
            log.warn("写入 Redis 缓存失败，忽略缓存异常，key={}", key, ex);
        }
    }

    public void evict(String key) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("删除 Redis 缓存失败，key={}", key, ex);
        }
    }
}
