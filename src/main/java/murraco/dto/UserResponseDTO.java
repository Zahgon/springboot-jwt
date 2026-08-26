package murraco.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.Data;
import murraco.model.AppUserRole;

@Data
public class UserResponseDTO {

  @Schema
  private Integer id;

  @Schema
  private String username;

  @Schema
  private String email;

  @Schema
  List<AppUserRole> appUserRoles;
}
