package com.liriosbeauty.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration:86400000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    // Extract username from token
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract specific claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Generate token for user
    public String generateToken(UserDetails userDetails) {
        return generateAccessToken(userDetails);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "access");
        return generateToken(claims, userDetails, accessExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("token_type", "refresh");
        return generateToken(claims, userDetails, refreshExpirationMs);
    }

    // Generate token with extra claims
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, accessExpirationMs);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        return buildToken(extraClaims, userDetails, expirationMs);
    }

    // Build JWT token
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    // Validate token
    public boolean isTokenValid(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails, "access");
    }

    public boolean isTokenValid(String token, UserDetails userDetails, String expectedType) {
        final String username = extractUsername(token);
        final String tokenType = extractClaim(token, claims -> claims.get("token_type", String.class));
        return (username.equals(userDetails.getUsername()))
                && !isTokenExpired(token)
                && expectedType.equals(tokenType);
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    // Check if token is expired
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extract expiration date
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Extract all claims from token
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Get signing key
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}