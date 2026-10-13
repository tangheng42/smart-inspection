package com.zhixunyun.utils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** 使用同一 HMAC 密钥签发并验证 JWT；有效期由调用方传入，单位毫秒。 */
public class JwtUtil {

    /** 签入用户声明及签发、到期时间。 */
    public static String createJWT(
            String secretKey,
            long ttl,
            Map<String, Object> claims) {

        long now = System.currentTimeMillis();

        Date expiration = new Date(now + ttl);

        SecretKey key = Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /** 验证签名并解析声明；无效或过期令牌由调用方转换为鉴权错误。 */
    public static Claims parseJWT(
            String secretKey,
            String token) {

        SecretKey key = Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
