package io.andy.shorten_url.auth.token;

import io.andy.shorten_url.auth.token.dto.CreateTokenDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.TokenExpiredException;

import io.jsonwebtoken.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.sql.Date;
import java.time.Instant;

@Slf4j
@Service
public class TokenService {
    private final SecretKey secretKey;
    private final String subject;

    public TokenService() {
        this.secretKey = Jwts.SIG.HS256.key().build();
        this.subject = "whitebox";
    }

    public String createToken(CreateTokenDto createTokenDto) {
        Instant current = Instant.now();
        Instant expiration = current.plusMillis(createTokenDto.getTokenLiveTime());

        return Jwts.builder()
                .subject(this.subject)
                .claim("userId", createTokenDto.getUserId())
                .claim("userAgent", createTokenDto.getUserAgent())
                .claim("ipAddress", createTokenDto.getIpAddress())
                .issuedAt(Date.from(current))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public VerifyTokenDto verifyToken(String accessToken) {
        // validate token expiration
        if (isTokenExpired(accessToken)) {
            throw new TokenExpiredException();
        }

        // parse claims from token
        Long userId = parsePayload(accessToken, "userId", Long.class);
        String userAgent = parsePayload(accessToken, "userAgent", String.class);
        String ipAddress = parsePayload(accessToken, "ipAddress", String.class);

        // check token server
        if (isInvalidTokenSubject(accessToken)) {
            log.debug("wrong token subject while verify token, userId={}", userId);
            throw new BadRequestException("WRONG TOKEN REQUEST");
        }

        return VerifyTokenDto.build(userId, ipAddress, userAgent, accessToken);
    }

    private boolean isTokenExpired(String token) {
        try {
            return parseClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date(System.currentTimeMillis()));
        } catch (TokenExpiredException e) {
            return true;
        }
    }

    private boolean isInvalidTokenSubject(String token) {
        return !parseClaims(token)
                .getPayload()
                .getSubject()
                .equals(this.subject);
    }

    private <T> T parsePayload(String token, String claim, Class<T> clazz) {
        return parseClaims(token)
                .getPayload()
                .get(claim, clazz);
    }

    private Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey).build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            log.debug("expired token while parse claims jwt");
            throw new TokenExpiredException();
        } catch (JwtException e) {
            throw new UnauthorizedException(e.getMessage());
        }
    }
}
