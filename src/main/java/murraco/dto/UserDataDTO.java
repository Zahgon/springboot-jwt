package murraco.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import murraco.model.AppUserRole;

@Data
@NoArgsConstructor
public class UserDataDTO {

  @Schema
  @NotBlank
  @Size(min = 4, max = 255, message = "Minimum username length: 4 characters")
  private String username;

  @Schema
  @NotBlank
  @Email
  private String email;

  @Schema
  @NotBlank
  @Size(min = 8, message = "Minimum password length: 8 characters")
  private String password;

  @Schema
  private List<AppUserRole> appUserRoles;
}
