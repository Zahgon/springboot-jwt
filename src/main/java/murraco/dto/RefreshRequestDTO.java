package murraco.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of the refresh and logout requests. */
@Data
@NoArgsConstructor
public class RefreshRequestDTO {

  @Schema(description = "The refresh token issued alongside the current access token")
  @NotBlank
  private String refreshToken;
}
