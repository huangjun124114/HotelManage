package com.linxi.security;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.token-prefix}")
    private String tokenPrefix;

    /**
     * 生成Token（不带角色权限）
     */
    public String generateToken(Long userId, String username, String realName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("realName", realName);
        return generateToken(claims);
    }

    /**
     * 生成Token（带角色权限）
     */
    public String generateToken(Long userId, String username, String realName, java.util.List<String> roles, java.util.List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("realName", realName);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        return generateToken(claims);
    }

    private String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            return (Long) userId;
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            return (String) claims.get("username");
        }
        return null;
    }

    /**
     * 从Token中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object roles = claims.get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 从Token中获取权限列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            Object permissions = claims.get("permissions");
            if (permissions instanceof List) {
                return (List<String>) permissions;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 解析Token
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT Token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT Token签名无效: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT Token参数非法: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        return getClaimsFromToken(token) != null;
    }

    /**
     * 从请求头获取Token（去除Bearer前缀）
     */
    public String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            return authHeader.substring(tokenPrefix.length());
        }
        return authHeader;
    }

    public String getHeader() {
        return header;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }
}
