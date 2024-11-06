package io.andy.shorten_url.auth.token;

import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.TokenExpiredException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Date;

@Service
public class TokenService {
    private final SecretKey secretKey;

    public TokenService() {
        this.secretKey = Jwts.SIG.HS256.key().build();
    }

    public String createToken(TokenType tokenType, CreateAuthTokenRequestDto createAuthTokenRequestDto, long tokenTtl) {
        String tokenSubject = convertTokenEnums(tokenType);
        Date current = new Date(System.currentTimeMillis());
        Date expiration = new Date(current.getTime() + tokenTtl);

        return Jwts.builder()
                .subject(tokenSubject)
                .claim("userId", createAuthTokenRequestDto.userId())
                .claim("userAgent", createAuthTokenRequestDto.userAgent())
                .issuedAt(current)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public boolean verifyToken(TokenType tokenType, VerifyAuthTokenDto verifyAuthTokenDto) {
        // 토큰 만료 확인
        if (isTokenExpired(verifyAuthTokenDto.token())) {
            throw new TokenExpiredException();
        }
        // 토큰 타입 확인 (AccessToken/RefreshToken)
        String subject = convertTokenEnums(tokenType);
        if (isInvalidTokenSubject(subject, verifyAuthTokenDto.token())) {
            throw new UnauthorizedException("NOT ACCESS TOKEN");
        }
        // 토큰 claims 확인
        Long userId = parsePayload(verifyAuthTokenDto.token(), "userId", Long.class);
        String userAgent = parsePayload(verifyAuthTokenDto.token(), "userAgent", String.class);
        if (!userId.equals(verifyAuthTokenDto.userId()) ||
                !userAgent.equals(verifyAuthTokenDto.userAgent())) {
            throw new UnauthorizedException("UNAUTHORIZED TOKEN");
        }
        return true;
    }

    public String convertTokenEnums(TokenType tokenType) {
        return switch (tokenType) {
            case ACCESS_TOKEN -> "access";
            case REFRESH_TOKEN -> "refresh";
        };
    }

    private boolean isTokenExpired(String token) {
        try {
            return parseClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date(System.currentTimeMillis()));
        } catch (TokenExpiredException e) {
            return false;
        }
    }

    private boolean isInvalidTokenSubject(String subject, String token) {
        return !parseClaims(token)
                .getPayload()
                .getSubject()
                .equals(subject);
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
        } catch (JwtException e) {
            if (e.getMessage().contains("expired")) {
                throw new TokenExpiredException();
            }
            throw new UnauthorizedException(e.getMessage());
        }
    }
}
