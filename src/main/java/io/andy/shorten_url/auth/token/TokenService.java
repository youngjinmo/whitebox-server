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

    public String createToken(CreateTokenDto createTokenDto, long tokenTtl) {
        Instant current = Instant.now();
        Instant expiration = current.plusMillis(tokenTtl);

        return Jwts.builder()
                .subject(this.subject)
                .claim("userId", createTokenDto.userId())
                .claim("userAgent", createTokenDto.userAgent())
                .claim("ipAddress", createTokenDto.ipAddress())
                .issuedAt(Date.from(current))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public void verifyToken(VerifyTokenDto verifyTokenDto) {
        // 토큰 만료 확인
        if (isTokenExpired(verifyTokenDto.token())) {
            throw new TokenExpiredException();
        }
        // 토큰 발행 서버 확인
        if (isInvalidTokenSubject(verifyTokenDto.token())) {
            log.debug("wrong token subject while verify token, userId={}", verifyTokenDto.userId());
            throw new BadRequestException("WRONG TOKEN REQUEST");
        }
        // 토큰 claims 확인
        Long userId = parsePayload(verifyTokenDto.token(), "userId", Long.class);
        String userAgent = parsePayload(verifyTokenDto.token(), "userAgent", String.class);
        String ipAddress = parsePayload(verifyTokenDto.token(), "ipAddress", String.class);
        if (!userId.equals(verifyTokenDto.userId()) ||
                !userAgent.equals(verifyTokenDto.userAgent()) ||
                !ipAddress.equals(verifyTokenDto.ipAddress())
        ) {
            throw new UnauthorizedException("UNAUTHORIZED TOKEN");
        }
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
