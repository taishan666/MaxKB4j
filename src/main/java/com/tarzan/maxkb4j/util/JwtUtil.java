package com.tarzan.maxkb4j.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtUtil {

    private static final String SECRET = "maxKB4j"; // 使用你的密钥替换


    public static String createToken(Map<String, Object> params) {
        JwtBuilder builder = Jwts.builder().setHeaderParam("typ", "JsonWebToken")
                .setIssuer("maxKB4j")
               // .setAudience("")
                .signWith(SignatureAlgorithm.HS512, SECRET.getBytes());
        params.forEach(builder::claim);
        return builder.compact();
    }

    public static Claims parseToken(String token) throws SignatureException {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET.getBytes())
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SignatureException e) {
            log.error("Invalid JWT signature");
        } catch (Exception e) {
            log.error("Unable to parse JWT token");
        }
        return new DefaultClaims();
    }

    public static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

 /*   public static void main(String[] args) {
        String token = JwtUtil.createToken("user123");
        System.out.println("Generated Token: " + token);

        try {
            Claims claims = JwtUtil.parseToken(token);
            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Is Expired: " + isTokenExpired(claims));
        } catch (SignatureException e) {
            System.out.println(e.getMessage());
        }
    }*/
}
