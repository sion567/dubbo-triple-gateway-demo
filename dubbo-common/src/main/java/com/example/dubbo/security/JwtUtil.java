package com.example.dubbo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具：HS256 签名，含用户名 + 角色 + 过期时间。
 * Secret 通过环境变量 JWT_SECRET 覆盖（长度必须 >= 32 字节）。
 */
public final class JwtUtil {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMS = "perms";

    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET", "demo-jwt-secret-0123456789-abcdefghijklmnop");
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final long DEFAULT_TTL_MILLIS = 2 * 60 * 60 * 1000L; // 2 小时

    private JwtUtil() {
    }

    public static String createToken(String username, List<String> roles, List<String> perms) {
        return createToken(username, roles, perms, DEFAULT_TTL_MILLIS);
    }

    public static String createToken(String username, List<String> roles, List<String> perms, long ttlMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_PERMS, perms)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析并验签。签名不符 / 过期 / 格式错误都会抛 JwtException 子类。
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
