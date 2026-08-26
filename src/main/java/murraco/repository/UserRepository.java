package murraco.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import murraco.model.AppUser;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<AppUser, Integer> {

  public boolean existsByUsername(String username) {
    return count("username", username) > 0;
  }

  public AppUser findByUsername(String username) {
    return find("username", username).firstResult();
  }

  @Transactional
  public void save(AppUser appUser) {
    persist(appUser);
  }

  @Transactional
  public void deleteByUsername(String username) {
    delete("username", username);
  }
}
