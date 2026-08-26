package murraco.security;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import murraco.exception.CustomException;

@Provider
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class JwtAuthenticationFilter implements ContainerRequestFilter {

  private static final Set<String> PUBLIC_PATHS =
      Set.of("users/signin", "users/signup", "users/refresh", "users/logout");

  private static final String BEARER_PREFIX = "Bearer ";

  @Inject
  JwtTokenProvider jwtTokenProvider;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = normalize(requestContext.getUriInfo().getPath());

    if (PUBLIC_PATHS.contains(path)) {
      return;
    }

    boolean secure = requestContext.getSecurityContext() != null
        && requestContext.getSecurityContext().isSecure();

    String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
      requestContext.setSecurityContext(anonymous(secure));
      return;
    }

    String token = authHeader.substring(BEARER_PREFIX.length());
    try {
      jwtTokenProvider.validateToken(token);
    } catch (CustomException ex) {
      requestContext.abortWith(
          Response.status(ex.getStatus()).entity(ex.getMessage()).build());
      return;
    }

    String username = jwtTokenProvider.getUsername(token);
    List<String> roles = jwtTokenProvider.getRoles(token);
    requestContext.setSecurityContext(authenticated(username, roles, secure));
  }

  private static String normalize(String path) {
    if (path == null) {
      return "";
    }
    int start = 0;
    int end = path.length();
    while (start < end && path.charAt(start) == '/') {
      start++;
    }
    while (end > start && path.charAt(end - 1) == '/') {
      end--;
    }
    return path.substring(start, end);
  }

  private static SecurityContext anonymous(boolean secure) {
    return new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return null;
      }

      @Override
      public boolean isUserInRole(String role) {
        return false;
      }

      @Override
      public boolean isSecure() {
        return secure;
      }

      @Override
      public String getAuthenticationScheme() {
        return null;
      }
    };
  }

  private static SecurityContext authenticated(String username, List<String> roles, boolean secure) {
    Principal principal = () -> username;
    return new SecurityContext() {
      @Override
      public Principal getUserPrincipal() {
        return principal;
      }

      @Override
      public boolean isUserInRole(String role) {
        return roles.contains(role);
      }

      @Override
      public boolean isSecure() {
        return secure;
      }

      @Override
      public String getAuthenticationScheme() {
        return "Bearer";
      }
    };
  }
}
