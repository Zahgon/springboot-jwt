package murraco.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import murraco.exception.CustomException;
import murraco.model.AppUserRole;

@ApplicationScoped
public class JwtTokenProvider {

  @ConfigProperty(name = "security.jwt.token.secret-key", defaultValue = "secret-key")
  String secretKey;

  @ConfigProperty(name = "security.jwt.token.expire-length", defaultValue = "3600000")
  long validityInMilliseconds;

  private SecretKey signingKey;

  @PostConstruct
  protected void init() {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(secretKey.getBytes(StandardCharsets.UTF_8));
      this.signingKey = Keys.hmacShaKeyFor(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Unable to initialize JWT signing key", e);
    }
  }

  public String createToken(String username, List<AppUserRole> appUserRoles) {
    List<String> roles = appUserRoles.stream().map(AppUserRole::name).toList();

    Date now = new Date();
    Date validity = new Date(now.getTime() + validityInMilliseconds);

    return Jwts.builder()
        .subject(username)
        .claim("auth", roles)
        .issuedAt(now)
        .expiration(validity)
        .signWith(signingKey)
        .compact();
  }

  public long getValidityInSeconds() {
    return validityInMilliseconds / 1000;
  }

  public String getUsername(String token) {
    return parse(token).getSubject();
  }

  @SuppressWarnings("unchecked")
  public List<String> getRoles(String token) {
    Object auth = parse(token).get("auth");
    if (auth instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of();
  }

  public boolean validateToken(String token) {
    try {
      parse(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      throw new CustomException("Expired or invalid JWT token", 401);
    }
  }

  private Claims parse(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
