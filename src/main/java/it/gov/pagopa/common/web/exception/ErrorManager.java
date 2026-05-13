package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class ErrorManager {
  private final ErrorDTO defaultErrorDTO;

  public ErrorManager(@Nullable ErrorDTO defaultErrorDTO) {
    this.defaultErrorDTO = Optional.ofNullable(defaultErrorDTO)
            .orElse(new ErrorDTO("Error", "Something gone wrong"));
  }

  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, ServerHttpRequest request) {

    logClientException(error, request);

    if(error instanceof ClientExceptionNoBody clientExceptionNoBody){
      return ResponseEntity.status(clientExceptionNoBody.getHttpStatus()).build();
    }
    else {
      ErrorDTO errorDTO;
      HttpStatus httpStatus;
      if (error instanceof ClientExceptionWithBody clientExceptionWithBody){
        httpStatus=clientExceptionWithBody.getHttpStatus();
        errorDTO = new ErrorDTO(clientExceptionWithBody.getCode(),  error.getMessage());
      }
      else {
        httpStatus=HttpStatus.INTERNAL_SERVER_ERROR;
        errorDTO = defaultErrorDTO;
      }
      return ResponseEntity.status(httpStatus)
              .contentType(MediaType.APPLICATION_JSON)
              .body(errorDTO);
    }
  }
  public static void logClientException(RuntimeException error, ServerHttpRequest request) {
    Throwable unwrappedException = error.getCause() instanceof ServiceException
            ? error.getCause()
            : error;

    String clientExceptionMessage = "";
    if(error instanceof ClientException clientException) {
      clientExceptionMessage = ": HttpStatus %s - %s%s".formatted(
              clientException.getHttpStatus(),
              (clientException instanceof ClientExceptionWithBody clientExceptionWithBody) ? clientExceptionWithBody.getCode() + ": " : "",
              clientException.getMessage()
      );
    }

    if(!(error instanceof ClientException clientException) || clientException.isPrintStackTrace() || unwrappedException.getCause() != null){
      log.error("Something went wrong handling request {}{}", getRequestDetails(request), clientExceptionMessage, unwrappedException);
    } else {
      log.info("A {} occurred handling request {}{} at {}",
              unwrappedException.getClass().getSimpleName() ,
              getRequestDetails(request),
              clientExceptionMessage,
              unwrappedException.getStackTrace().length > 0 ? unwrappedException.getStackTrace()[0] : "UNKNOWN");
    }
  }

  /**
    * Extracts and formats HTTP request details (Method and URI) for logging purposes,
    * automatically applying masking logic to sensitive Personally Identifiable Information (PII).
    *
    * <p>This method scans the request URI for strings matching the <b>Italian Fiscal Code</b>
    * pattern, including alphanumeric variants resulting from "omocodia" (homocode) management.
    * Any detected occurrence is replaced with a protection mask ({@code ******}) to ensure
    * GDPR compliance and prevent personal data leakage into application logs.</p>
    *
    * <p><b>Transformation Example:</b></p>
    * <pre>
    * Input:  GET https://api.pagopa.it/emd/citizen/RSSMRA85T10A562S/consent
    * Output: GET https://api.pagopa.it/emd/citizen/{@code ******}/consent}
    * </pre>
    *
    * @param request the {@link ServerHttpRequest} object from which to extract details.
    * @return a formatted {@link String} as "METHOD URI", with sensitive data obscured.
    */
  public static String getRequestDetails(ServerHttpRequest request) {
    String uri = request.getURI().toString();

    // Regex for Italian Fiscal Code (handles standard and homocode variants)
    String fiscalCodeRegex = "([A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z])";

    // Masking sensitive occurrences
    String maskedUri = uri.replaceAll(fiscalCodeRegex, "******");
    
    return "%s %s".formatted(request.getMethod(), maskedUri);
  }
}
