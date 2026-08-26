package murraco.controller;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserControllerTest {

  private static final String ADMIN_USER = "admin";
  private static final String ADMIN_PASSWORD = "admin123456";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode signin() throws Exception {
    String body = given()
        .contentType(ContentType.URLENC)
        .formParam("username", ADMIN_USER)
        .formParam("password", ADMIN_PASSWORD)
        .when()
        .post("/users/signin")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();
    return objectMapper.readTree(body);
  }

  private String refreshBody(String refreshToken) throws Exception {
    return objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
  }

  @Test
  void signin_withValidCredentials_returnsTokenPair() throws Exception {
    JsonNode tokens = signin();
    assertTrue(tokens.get("accessToken").asText().length() > 20, "Expected a JWT access token");
    assertTrue(tokens.get("refreshToken").asText().length() > 20, "Expected a refresh token");
    assertTrue(tokens.get("expiresIn").asLong() > 0, "Expected a positive access token lifetime");
  }

  @Test
  void me_withoutToken_returns403() {
    given()
        .when()
        .get("/users/me")
        .then()
        .statusCode(403);
  }

  @Test
  void me_withValidToken_returnsUserData() throws Exception {
    String accessToken = signin().get("accessToken").asText();
    given()
        .header("Authorization", "Bearer " + accessToken)
        .when()
        .get("/users/me")
        .then()
        .statusCode(200)
        .body("username", is(ADMIN_USER))
        .body("email", is("admin@email.com"))
        .body("appUserRoles", instanceOf(List.class));
  }

  @Test
  void refresh_withValidRefreshToken_returnsNewPair() throws Exception {
    JsonNode tokens = signin();
    String refreshToken = tokens.get("refreshToken").asText();

    String body = given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();
    JsonNode refreshed = objectMapper.readTree(body);

    assertTrue(refreshed.get("accessToken").asText().length() > 20, "Expected a new access token");
    assertNotEquals(refreshToken, refreshed.get("refreshToken").asText(), "Refresh token should rotate");

    given()
        .header("Authorization", "Bearer " + refreshed.get("accessToken").asText())
        .when()
        .get("/users/me")
        .then()
        .statusCode(200)
        .body("username", is(ADMIN_USER));
  }

  @Test
  void refresh_worksWithoutAccessToken() throws Exception {
    String refreshToken = signin().get("refreshToken").asText();
    given()
        .header("Authorization", "Bearer expired-and-invalid")
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(200);
  }

  @Test
  void refresh_reusingRotatedToken_returns401() throws Exception {
    String refreshToken = signin().get("refreshToken").asText();

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void refresh_afterReuseDetection_revokesRemainingTokens() throws Exception {
    String refreshToken = signin().get("refreshToken").asText();

    String body = given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();
    String rotated = objectMapper.readTree(body).get("refreshToken").asText();

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(401);

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(rotated))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void refresh_withUnknownToken_returns401() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body(refreshBody("not-a-real-refresh-token"))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void refresh_withBlankToken_returns400() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(""))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(400);
  }

  @Test
  void logout_revokesRefreshToken() throws Exception {
    String refreshToken = signin().get("refreshToken").asText();

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/logout")
        .then()
        .statusCode(204);

    given()
        .contentType(ContentType.JSON)
        .body(refreshBody(refreshToken))
        .when()
        .post("/users/refresh")
        .then()
        .statusCode(401);
  }

  @Test
  void logout_withUnknownToken_isIdempotent() throws Exception {
    given()
        .contentType(ContentType.JSON)
        .body(refreshBody("never-issued"))
        .when()
        .post("/users/logout")
        .then()
        .statusCode(204);
  }

  @Test
  void signup_returnsTokenPair() throws Exception {
    String body = "{\"username\":\"newuser\",\"email\":\"newuser@example.com\",\"password\":\"password12\",\"appUserRoles\":[\"ROLE_CLIENT\"]}";
    String response = given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/users/signup")
        .then()
        .statusCode(200)
        .extract()
        .body()
        .asString();
    JsonNode tokens = objectMapper.readTree(response);
    assertTrue(tokens.get("accessToken").asText().length() > 20, "Expected a JWT access token");
    assertTrue(tokens.get("refreshToken").asText().length() > 20, "Expected a refresh token");
  }

  @Test
  void signup_duplicateUsername_returns422() {
    String body = "{\"username\":\"admin\",\"email\":\"other@example.com\",\"password\":\"password12\",\"appUserRoles\":[\"ROLE_CLIENT\"]}";
    given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/users/signup")
        .then()
        .statusCode(422);
  }

  @Test
  void me_withMalformedToken_returns401() {
    given()
        .header("Authorization", "Bearer not-a-valid-jwt")
        .when()
        .get("/users/me")
        .then()
        .statusCode(401);
  }
}
