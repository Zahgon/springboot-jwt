package murraco.controller;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.modelmapper.ModelMapper;

import murraco.dto.AuthResponseDTO;
import murraco.dto.RefreshRequestDTO;
import murraco.dto.UserDataDTO;
import murraco.dto.UserResponseDTO;
import murraco.exception.CustomException;
import murraco.model.AppUser;
import murraco.service.UserService;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "users")
public class UserResource {

  private static final String ROLE_ADMIN = "ROLE_ADMIN";
  private static final String ROLE_CLIENT = "ROLE_CLIENT";

  @Inject
  UserService userService;

  @Inject
  ModelMapper modelMapper;

  @POST
  @Path("/signin")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully signed in",
          content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "422", description = "Invalid username/password supplied")
  })
  public AuthResponseDTO login(@RestForm String username, @RestForm String password) {
    return userService.signin(username, password);
  }

  @POST
  @Path("/signup")
  @Consumes(MediaType.APPLICATION_JSON)
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully signed up",
          content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "403", description = "Access denied"),
      @APIResponse(responseCode = "422", description = "Username is already in use")
  })
  public AuthResponseDTO signup(@Valid UserDataDTO user) {
    return userService.signup(modelMapper.map(user, AppUser.class));
  }

  @DELETE
  @Path("/{username}")
  @Produces(MediaType.TEXT_PLAIN)
  @SecurityRequirement(name = "bearerAuth")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully deleted the user"),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "403", description = "Access denied"),
      @APIResponse(responseCode = "404", description = "The user doesn't exist"),
      @APIResponse(responseCode = "401", description = "Expired or invalid JWT token")
  })
  public String delete(@PathParam("username") String username, @Context SecurityContext securityContext) {
    requireRole(securityContext, ROLE_ADMIN);
    userService.delete(username);
    return username;
  }

  @GET
  @Path("/{username}")
  @SecurityRequirement(name = "bearerAuth")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully retrieved the user",
          content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "403", description = "Access denied"),
      @APIResponse(responseCode = "404", description = "The user doesn't exist"),
      @APIResponse(responseCode = "401", description = "Expired or invalid JWT token")
  })
  public UserResponseDTO search(@PathParam("username") String username, @Context SecurityContext securityContext) {
    requireRole(securityContext, ROLE_ADMIN);
    return modelMapper.map(userService.search(username), UserResponseDTO.class);
  }

  @GET
  @Path("/me")
  @SecurityRequirement(name = "bearerAuth")
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully retrieved the current user",
          content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "403", description = "Access denied"),
      @APIResponse(responseCode = "401", description = "Expired or invalid JWT token")
  })
  public UserResponseDTO whoami(@Context SecurityContext securityContext) {
    requireAnyRole(securityContext, ROLE_ADMIN, ROLE_CLIENT);
    String username = securityContext.getUserPrincipal().getName();
    return modelMapper.map(userService.whoami(username), UserResponseDTO.class);
  }

  @POST
  @Path("/refresh")
  @Consumes(MediaType.APPLICATION_JSON)
  @APIResponses({
      @APIResponse(responseCode = "200", description = "Successfully refreshed the tokens",
          content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
      @APIResponse(responseCode = "400", description = "Something went wrong"),
      @APIResponse(responseCode = "401", description = "Invalid or expired refresh token"),
      @APIResponse(responseCode = "404", description = "The user doesn't exist")
  })
  public AuthResponseDTO refresh(
      @RequestBody(content = @Content(schema = @Schema(implementation = RefreshRequestDTO.class)))
      @Valid RefreshRequestDTO request) {
    return userService.refresh(request.getRefreshToken());
  }

  @POST
  @Path("/logout")
  @Consumes(MediaType.APPLICATION_JSON)
  @APIResponses({
      @APIResponse(responseCode = "204", description = "Successfully logged out"),
      @APIResponse(responseCode = "400", description = "Something went wrong")
  })
  public Response logout(
      @RequestBody(content = @Content(schema = @Schema(implementation = RefreshRequestDTO.class)))
      @Valid RefreshRequestDTO request) {
    userService.logout(request.getRefreshToken());
    return Response.noContent().build();
  }

  private void requireRole(SecurityContext securityContext, String role) {
    if (securityContext.getUserPrincipal() == null || !securityContext.isUserInRole(role)) {
      throw new CustomException("Access denied", 403);
    }
  }

  private void requireAnyRole(SecurityContext securityContext, String... roles) {
    if (securityContext.getUserPrincipal() == null) {
      throw new CustomException("Access denied", 403);
    }
    for (String role : roles) {
      if (securityContext.isUserInRole(role)) {
        return;
      }
    }
    throw new CustomException("Access denied", 403);
  }
}
