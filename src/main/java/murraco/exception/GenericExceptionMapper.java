package murraco.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger log = LoggerFactory.getLogger(GenericExceptionMapper.class);

  @Override
  public Response toResponse(Exception ex) {
    if (ex instanceof WebApplicationException wae) {
      return wae.getResponse();
    }
    log.error("Unhandled exception", ex);
    return Response.status(500)
        .entity(new ErrorResponse(500, "Something went wrong"))
        .build();
  }
}
