package com.proyecto.servicios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expiracionHoras;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiracion-horas}") long expiracionHoras) {
        this.signingKey = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(secret));
        this.expiracionHoras = expiracionHoras;
    }

    public String generarToken(Long clienteId, String usuario) {
        Date ahora = new Date();
        Date expira = Date.from(
                LocalDateTime.now().plusHours(expiracionHoras).atZone(ZoneId.systemDefault()).toInstant()
        );
        return Jwts.builder()
                .subject(usuario)
                .claim("clienteId", clienteId)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(signingKey)
                .compact();
    }

    public LocalDateTime obtenerFechaExpiracion(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}