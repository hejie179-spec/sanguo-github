package com.sanguo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT工具类，用来生成和解析token
    // @Component：把这个类交给Spring管理，这样其他地方可以通过@Resource注入使用
    // @Value：从配置文件中读取配置，这样secret和expiration就不用硬编码在代码里了
    // secret：签名密钥，用来保证token的安全性
    // expiration：token的过期时间，单位是毫秒
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    // 获取签名密钥的方法
    // 为什么要处理keyBytes？因为HS256算法要求密钥至少32字节
    // 如果配置的secret不够长，就用0填充到32字节
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成token的方法
    // userId：用户ID，作为token的subject
    // username：用户名，作为token的一个claim
    public String generateToken(Integer userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setSubject(String.valueOf(userId)) // subject存用户ID，JwtFilter会用它加载用户信息
                .claim("username", username) // 额外存储用户名，方便调试和显示
                .setIssuedAt(now) // token的签发时间
                .setExpiration(expiry) // token的过期时间
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // 使用HS256算法签名
                .compact(); // 压缩成最终的token字符串
    }

    // 解析token的方法，返回token中的claims（声明）
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey()) // 设置签名密钥，用于验证token的真实性
                .build()
                .parseClaimsJws(token) // 解析token
                .getBody(); // 获取claims
    }

    // 从token中获取用户ID的方法
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Integer.parseInt(claims.getSubject()); // subject就是用户ID
    }

    // 验证token是否有效的方法
    // 如果解析成功，说明token有效；如果解析失败，说明token无效
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
