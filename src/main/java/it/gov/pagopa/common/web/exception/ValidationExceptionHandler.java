package it.gov.pagopa.common.web.exception;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;

import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ValidationExceptionHandler {

    private final ErrorDTO templateValidationErrorDTO;

    public ValidationExceptionHandler(@Nullable ErrorDTO templateValidationErrorDTO) {
        this.templateValidationErrorDTO = Optional.ofNullable(templateValidationErrorDTO)
                .orElse(new ErrorDTO("INVALID_REQUEST", "Invalid request"));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleWebExchangeBindException(
            WebExchangeBindException e) {

        String message = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    return String.format("[%s]: %s", fieldName, errorMessage);
                }).collect(Collectors.joining("; "));

        log.info("A WebExchangeBindException occurred : HttpStatus 400 - {}", message);
        log.debug("Something went wrong while validating http request", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), message);
    }

    @ExceptionHandler(MissingRequestValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleMissingRequestValueException(MissingRequestValueException e) {
        log.info("A MissingRequestValueException occurred : HttpStatus 400 - Something went wrong due to a missing request value");
        log.debug("Something went wrong due to a missing request value", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Something went wrong due to a missing request value");
    }
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleNoResourceFoundException(NoResourceFoundException e) {

        log.info("A NoResourceFoundException occurred : HttpStatus 400 - Something went wrong due to a missing static resource");
        log.debug("Something went wrong due to a missing static resource", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Something went wrong due to a missing static resource");
    }

    @ExceptionHandler(MethodNotAllowedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorDTO handleMethodNotAllowedException(MethodNotAllowedException e) {

        log.info("A MethodNotAllowedException occurred : HttpStatus 405 - Request is not supported");
        log.debug("Something went wrong due to a request not supported", e);

        return new ErrorDTO(templateValidationErrorDTO.getCode(), "Request is not supported");
    }


}
