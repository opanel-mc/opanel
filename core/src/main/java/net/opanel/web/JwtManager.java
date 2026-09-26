package net.opanel.web;

import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.jsonwebtoken.*;
import net.opanel.utils.Utils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JwtManager {
    private static final SecretKey signKey = Jwts.SIG.HS256.key().build();
    private static final Header accessKeyHeader = Jwts.header()
            .keyId("accessKey")
            .build();
    private static final String issuer = "opanel";
    private static final long SESSION_LIFETIME = TimeUnit.DAYS.toMillis(1);
    private static final long SESSION_CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    private static final ConcurrentHashMap<String, Long> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_SESSION_CLEANUP_AT = new AtomicLong();

    public static String generateToken(String hashedAccessKey, String salt) {
        final String access = Utils.md5(salt + hashedAccessKey); // salted hashed 3
        Date current = new Date();
        Date expiration = new Date(current.getTime() + SESSION_LIFETIME);
        cleanupExpiredSessions(current.getTime());

        while(true) {
            String sessionId = Utils.generateRandomHex(16);
            String token = Jwts.builder()
                    .header()
                        .add(accessKeyHeader)
                    .and()
                    .issuer(issuer)
                    .id(sessionId)
                    .expiration(expiration)
                    .issuedAt(current)
                    .claim("access", access)
                    .signWith(signKey)
                    .compact();

            if(ACTIVE_SESSIONS.putIfAbsent(sessionId, expiration.getTime()) == null) {
                return token;
            }
        }
    }

    public static boolean verifyToken(String token, String hashedAccessKey, String salt) {
        return getVerifiedClaims(token, hashedAccessKey, salt) != null;
    }

    public static boolean revokeToken(String token, String hashedAccessKey, String salt) {
        Claims claims = getVerifiedClaims(token, hashedAccessKey, salt);
        if(claims == null) return false;

        String sessionId = claims.getId();
        Long expiration = ACTIVE_SESSIONS.get(sessionId);
        return expiration != null && ACTIVE_SESSIONS.remove(sessionId, expiration);
    }

    public static void revokeAllTokens() {
        ACTIVE_SESSIONS.clear();
    }

    private static Claims getVerifiedClaims(String token, String hashedAccessKey, String salt) {
        final String access = Utils.md5(salt + hashedAccessKey); // salted hashed 3
        long currentTime = System.currentTimeMillis();
        cleanupExpiredSessions(currentTime);

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signKey)
                    .build()
                    .parseSignedClaims(token);

            Claims payload = jws.getPayload();
            if(
                payload == null
                || payload.getId() == null
                || payload.getIssuer() == null
                || payload.getExpiration() == null
                || payload.get("access") == null
            ) {
                return null;
            }
            if(!"accessKey".equals(jws.getHeader().getKeyId())) return null;
            if(!issuer.equals(payload.getIssuer())) return null;
            if(!access.equals(payload.get("access"))) return null;

            Long sessionExpiration = ACTIVE_SESSIONS.get(payload.getId());
            if(sessionExpiration == null) return null;
            if(currentTime >= sessionExpiration) {
                ACTIVE_SESSIONS.remove(payload.getId(), sessionExpiration);
                return null;
            }

            return payload;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void cleanupExpiredSessions(long currentTime) {
        long nextCleanup = NEXT_SESSION_CLEANUP_AT.get();
        if(
            currentTime < nextCleanup
            || !NEXT_SESSION_CLEANUP_AT.compareAndSet(nextCleanup, currentTime + SESSION_CLEANUP_INTERVAL)
        ) {
            return;
        }

        ACTIVE_SESSIONS.entrySet().removeIf(entry -> entry.getValue() <= currentTime);
    }

    public static Cookie createCookie(String name, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setSameSite(SameSite.LAX);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        return cookie;
    }
}
