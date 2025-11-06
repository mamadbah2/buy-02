package sn.dev.order_service.exceptions;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class FeignExceptions extends ResponseEntityExceptionHandler {
  /**
   * Handles Feign's BadRequest (400) exceptions.
   * This captures the specific error you are seeing.
   */
  @ExceptionHandler(FeignException.BadRequest.class)
  public ResponseEntity<String> handleFeignBadRequest(FeignException.BadRequest e) {
    // e.contentUTF8() contains the original JSON error body from the media-service
    // e.g., {"error":"Validation Error","message":"Only JPEG, PNG, GIF and WEBP images are allowed."}
    return new ResponseEntity<>(e.contentUTF8(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles all other Feign client exceptions (e.g., 404 Not Found, 503 Service Unavailable).
   * This is a good general practice.
   */
  @ExceptionHandler(FeignException.class)
  public ResponseEntity<String> handleFeignStatusException(FeignException e) {
    String errorMessage = e.contentUTF8();
    HttpStatus status = HttpStatus.resolve(e.status());

    // Default to a generic server error if the status code is not recognized
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return new ResponseEntity<>(errorMessage, status);
  }
}
