package murraco.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import murraco.dto.AuthResponseDTO;
import murraco.exception.CustomException;
import murraco.model.AppUser;
import murraco.repository.UserRepository;
import murraco.security.JwtTokenProvider;
import murraco.service.RefreshTokenService.Rotation;

@ApplicationScoped
public class UserService {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  private static final String TOKEN_TYPE = "Bearer";

  private static final int BCRYPT_COST = 12;

  @Inject
  UserRepository userRepository;

  @Inject
  JwtTokenProvider jwtTokenProvider;

  @Inject
  RefreshTokenService refreshTokenService;

  public AuthResponseDTO signin(String username, String password) {
    AppUser appUser = userRepository.findByUsername(username);
    if (appUser == null
        || !BCrypt.verifyer().verify(password.toCharArray(), appUser.getPassword()).verified) {
      throw new CustomException("Invalid username/password supplied", 422);
    }
    log.info("User signed in: {}", username);
    return issueTokens(appUser);
  }

  public AuthResponseDTO signup(AppUser appUser) {
    return issueTokens(register(appUser));
  }

  public AppUser register(AppUser appUser) {
    if (userRepository.existsByUsername(appUser.getUsername())) {
      throw new CustomException("Username is already in use", 422);
    }
    appUser.setPassword(BCrypt.withDefaults().hashToString(BCRYPT_COST, appUser.getPassword().toCharArray()));
    userRepository.save(appUser);
    log.info("User signed up: {}", appUser.getUsername());
    return appUser;
  }

  @Transactional
  public void delete(String username) {
    refreshTokenService.deleteAllForUser(username);
    userRepository.deleteByUsername(username);
  }

  public AppUser search(String username) {
    AppUser appUser = userRepository.findByUsername(username);
    if (appUser == null) {
      throw new CustomException("The user doesn't exist", 404);
    }
    return appUser;
  }

  public AppUser whoami(String username) {
    AppUser appUser = userRepository.findByUsername(username);
    if (appUser == null) {
      throw new CustomException("The user doesn't exist", 404);
    }
    return appUser;
  }

  public AuthResponseDTO refresh(String refreshToken) {
    Rotation rotation = refreshTokenService.rotate(refreshToken);
    AppUser appUser = userRepository.findByUsername(rotation.username());
    if (appUser == null) {
      throw new CustomException("The user doesn't exist", 404);
    }
    String accessToken = jwtTokenProvider.createToken(appUser.getUsername(), appUser.getAppUserRoles());
    log.info("Refreshed tokens for user: {}", appUser.getUsername());
    return new AuthResponseDTO(accessToken, rotation.newRefreshToken(), TOKEN_TYPE,
        jwtTokenProvider.getValidityInSeconds());
  }

  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  private AuthResponseDTO issueTokens(AppUser appUser) {
    String accessToken = jwtTokenProvider.createToken(appUser.getUsername(), appUser.getAppUserRoles());
    String refreshToken = refreshTokenService.issue(appUser.getUsername());
    return new AuthResponseDTO(accessToken, refreshToken, TOKEN_TYPE, jwtTokenProvider.getValidityInSeconds());
  }
}
