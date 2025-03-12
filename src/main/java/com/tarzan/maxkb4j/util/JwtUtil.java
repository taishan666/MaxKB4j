package com.tarzan.maxkb4j.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JwtUtil {

    /**
     * 过期时间(单位:秒)
     */
    public static final int ACCESS_EXPIRE = 60;
    /**
     * 加密算法
     */
    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;
    /**
     * 私钥 / 生成签名的时候使用的秘钥secret，一般可以从本地配置文件中读取，切记这个秘钥不能外露，只在服务端使用，在任何场景都不应该流露出去。
     * 一旦客户端得知这个secret, 那就意味着客户端是可以自我签发jwt了。
     * 应该大于等于 256位(长度32及以上的字符串)，并且是随机的字符串
     */
    private final static String SECRET = "asdasdasifhueuiwyurfewbfjsdafjk";
    /**
     * 秘钥实例
     */
  //  public static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static final SecretKey KEY =  Keys.secretKeyFor(SignatureAlgorithm.HS256);
    /**
     * jwt签发者
     */
    private final static String JWT_ISS = "tarzan";
    /**
     * jwt主题
     */
    private final static String SUBJECT = "Peripherals";

    public static String createToken(Map<String, Object> params) {
        // 令牌id
        String uuid = UUID.randomUUID().toString();
        Date exprireDate = Date.from(Instant.now().plusSeconds(ACCESS_EXPIRE));
        JwtBuilder builder =Jwts.builder()
                // 设置头部信息header
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                // 令牌ID
                .id(uuid)
                // 过期日期
                .expiration(exprireDate)
                // 签发时间
                .issuedAt(new Date())
                // 主题
                .subject(SUBJECT)
                // 签发者
                .issuer(JWT_ISS)
                // 签名
                .signWith(KEY);
        params.forEach(builder::claim);
        return builder.compact();
    }


    /**
     * 解析token
     * @param token token
     * @return Jws<Claims>
     */
    public static Jws<Claims> parseClaim(String token) throws JwtException, IllegalArgumentException{
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token);
    }

    public static JwsHeader parseHeader(String token) {
        return parseClaim(token).getHeader();
    }

    public static Claims parseToken(String token) {
        Claims claims= parseClaim(token).getPayload();
        return claims==null?Jwts.claims().build():claims;
    }


    public static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

}
