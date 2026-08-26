package murraco.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CustomExceptionMapper implements ExceptionMapper<CustomException> {

  @Override
  public Response toResponse(CustomException ex) {
    return Response.status(ex.getStatus())
        .entity(new ErrorResponse(ex.getStatus(), ex.getMessage()))
        .build();
  }
}
