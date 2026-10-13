package com.zhixunyun.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {

    private static final String SECRET_KEY =
            "smart-inspection-test-secret-key-32-bytes";

    @Test
    void shouldCreateAndParseToken() {
        String token = JwtUtil.createJWT(
                SECRET_KEY,
                60_000L,
                Map.of("userId", 1L, "username", "admin", "roleCode", "ADMIN")
        );

        Claims claims = JwtUtil.parseJWT(SECRET_KEY, token);

        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("admin", claims.get("username", String.class));
        assertEquals("ADMIN", claims.get("roleCode", String.class));
    }
}
