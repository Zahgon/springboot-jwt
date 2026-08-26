package murraco.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import murraco.exception.CustomException;
import murraco.model.RefreshToken;
import murraco.repository.RefreshTokenRepository;

@ApplicationScoped
public class RefreshTokenService {

  private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

  private static final int TOKEN_BYTES = 32;

  @Inject
  RefreshTokenRepository refreshTokenRepository;

  private final SecureRandom secureRandom = new SecureRandom();

  @ConfigProperty(name = "security.jwt.refresh-token.expire-length", defaultValue = "604800000")
  long refreshValidityInMilliseconds;

  @Transactional
  public String issue(String username) {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setTokenHash(hash(rawToken));
    refreshToken.setUsername(username);
    refreshToken.setExpiryDate(Instant.now().plusMillis(refreshValidityInMilliseconds));
    refreshToken.setRevoked(false);
    refreshTokenRepository.save(refreshToken);

    return rawToken;
  }

  @Transactional(dontRollbackOn = CustomException.class)
  public Rotation rotate(String rawToken) {
    RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
        .orElseThrow(() -> new CustomException("Invalid refresh token", 401));

    String username = refreshToken.getUsername();

    if (refreshToken.isRevoked()) {
      log.warn("Refresh token reuse detected for user {}; revoking all tokens", username);
      refreshTokenRepository.revokeAllByUsername(username);
      throw new CustomException("Invalid refresh token", 401);
    }

    if (refreshToken.isExpired()) {
      throw new CustomException("Expired refresh token", 401);
    }

    refreshToken.setRevoked(true);
    refreshTokenRepository.save(refreshToken);

    return new Rotation(username, issue(username));
  }

  @Transactional
  public void revoke(String rawToken) {
    refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(refreshToken -> {
      refreshToken.setRevoked(true);
      refreshTokenRepository.save(refreshToken);
    });
  }

  @Transactional
  public void deleteAllForUser(String username) {
    refreshTokenRepository.deleteByUsername(username);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 algorithm not available", e);
    }
  }

  public record Rotation(String username, String newRefreshToken) {
  }
}
