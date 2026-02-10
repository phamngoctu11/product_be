package com.example.workflow.config;
import com.example.workflow.nume.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtils {
    private final String SECRET_KEY = "YourSecretKeyForJWTAuthenticationProjectWorkflow";
    private final long EXPIRATION_TIME = 86400000;
    public String generateToken(Long user_id,String username, Role role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("user_id",user_id)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}