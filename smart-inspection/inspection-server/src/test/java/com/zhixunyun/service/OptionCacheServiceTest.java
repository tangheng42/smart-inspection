package com.zhixunyun.service;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixunyun.vo.AreaOptionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OptionCacheServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OptionCacheService cacheService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new OptionCacheService(redisTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(cacheService, "enabled", true);
        ReflectionTestUtils.setField(cacheService, "optionTtl", Duration.ofMinutes(30));
    }

    @Test
    void shouldDeserializeCachedOptionList() {
        when(valueOperations.get(OptionCacheService.AREA_OPTIONS_KEY))
                .thenReturn("[{\"id\":1,\"code\":\"A001\",\"name\":\"一号区域\"}]");

        List<AreaOptionVO> options = cacheService.getList(
                OptionCacheService.AREA_OPTIONS_KEY, AreaOptionVO.class);

        assertEquals(1, options.size());
        assertEquals(1L, options.get(0).getId());
        assertEquals("A001", options.get(0).getCode());
    }

    @Test
    void shouldReturnCacheMissWhenRedisReadFails() {
        when(valueOperations.get(OptionCacheService.AREA_OPTIONS_KEY))
                .thenThrow(new IllegalStateException("redis unavailable"));

        List<AreaOptionVO> options = cacheService.getList(
                OptionCacheService.AREA_OPTIONS_KEY, AreaOptionVO.class);

        assertNull(options);
    }

    @Test
    void shouldNotBreakBusinessWhenRedisWriteOrDeleteFails() {
        org.mockito.Mockito.doThrow(new IllegalStateException("redis unavailable"))
                .when(valueOperations).set(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Duration.class));
        when(redisTemplate.delete(OptionCacheService.AREA_OPTIONS_KEY))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertDoesNotThrow(() -> cacheService.put(
                OptionCacheService.AREA_OPTIONS_KEY, List.of()));
        assertDoesNotThrow(() -> cacheService.evict(
                OptionCacheService.AREA_OPTIONS_KEY));
        verify(redisTemplate).delete(OptionCacheService.AREA_OPTIONS_KEY);
    }
}
