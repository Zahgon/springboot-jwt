package murraco;

import java.util.ArrayList;
import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.runtime.StartupEvent;

import murraco.model.AppUser;
import murraco.model.AppUserRole;
import murraco.repository.UserRepository;
import murraco.service.UserService;

@ApplicationScoped
public class StartupSeeder {

  @Inject
  UserService userService;

  @Inject
  UserRepository userRepository;

  @Transactional
  void onStart(@Observes StartupEvent ev) {
    if (!userRepository.existsByUsername("admin")) {
      AppUser admin = new AppUser();
      admin.setUsername("admin");
      admin.setPassword("admin123456");
      admin.setEmail("admin@email.com");
      admin.setAppUserRoles(new ArrayList<>(Arrays.asList(AppUserRole.ROLE_ADMIN)));
      userService.register(admin);
    }
    if (!userRepository.existsByUsername("client")) {
      AppUser client = new AppUser();
      client.setUsername("client");
      client.setPassword("client123456");
      client.setEmail("client@email.com");
      client.setAppUserRoles(new ArrayList<>(Arrays.asList(AppUserRole.ROLE_CLIENT)));
      userService.register(client);
    }
  }
}
