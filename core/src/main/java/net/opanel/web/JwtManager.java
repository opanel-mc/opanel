package net.opanel.web;

import io.jsonwebtoken.*;
import net.opanel.utils.Utils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class JwtManager {
    private static final SecretKey signKey = Jwts.SIG.HS256.key().build();
    private static final Header accessKeyHeader = Jwts.header()
            .keyId("accessKey")
            .build();
    private static final String issuer = "opanel";

    public static String generateToken(String hashedAccessKey, String salt) {
        final String access = Utils.md5(salt + hashedAccessKey); // salted hashed 3
        Date current = new Date();
        return Jwts.builder()
                .header()
                    .add(accessKeyHeader)
                .and()
                .issuer(issuer)
                .expiration(new Date(current.getTime() + TimeUnit.DAYS.toMillis(1)))
                .issuedAt(current)
                .claim("access", access)
                .signWith(signKey)
                .compact();
    }

    public static boolean verifyToken(String token, String hashedAccessKey, String salt) {
        final String access = Utils.md5(salt + hashedAccessKey); // salted hashed 3
        Date current = new Date();
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signKey)
                    .build()
                    .parseSignedClaims(token);
            if(!jws.getHeader().getKeyId().equals("accessKey")) return false;

            Claims payload = jws.getPayload();
            if(!payload.getIssuer().equals(issuer)) return false;
            if(current.after(payload.getExpiration())) return false;
            if(!payload.get("access").equals(access)) return false;
        } catch (JwtException e) {
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
