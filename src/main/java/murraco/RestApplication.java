package murraco;

import jakarta.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    info = @Info(
        title = "JSON Web Token Authentication API",
        version = "1.0.0",
        description = "Sample JWT authentication service. Demo users: `admin` / `admin123456` and `client` / `client123456`. After sign-in, use **Authorize** and enter `Bearer <token>`.",
        license = @License(name = "MIT License", url = "http://opensource.org/licenses/MIT"),
        contact = @Contact(email = "mauriurraco@gmail.com")))
@SecurityScheme(
    securitySchemeName = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class RestApplication extends Application {
}
