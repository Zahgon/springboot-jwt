package murraco.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.Optional;
import murraco.model.RefreshToken;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, Integer> {

  public Optional<RefreshToken> findByTokenHash(String tokenHash) {
    return find("tokenHash", tokenHash).firstResultOptional();
  }

  @Transactional
  public void save(RefreshToken refreshToken) {
    persist(refreshToken);
  }

  @Transactional
  public void deleteByUsername(String username) {
    delete("username", username);
  }

  @Transactional
  public int revokeAllByUsername(String username) {
    return update("revoked = true where username = ?1 and revoked = false", username);
  }
}
