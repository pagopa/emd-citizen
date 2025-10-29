package it.gov.pagopa.onboarding.citizen.configuration;

import it.gov.pagopa.common.web.exception.ClientException;
import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.constants.CitizenConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * <p>Central registry mapping symbolic exception keys to concrete {@link ClientException} factories.</p>
 *
 * <p>Provides a single lookup method to retrieve domain exceptions according to service specifications.</p>
 */
@Configuration
@Slf4j
public class ExceptionMap {

    /**
     * <p>Exception registry mapping logical keys to factory functions producing {@link ClientException} instances.</p>
     */
    private final Map<String, Function<String, ClientException>> exceptions = new HashMap<>();

    /**
     * <p>Initializes the registry with supported exception mappings.</p>
     *
     * <p>Each entry associates a symbolic key from {@link CitizenConstants.ExceptionName} with a factory function
     * that produces the corresponding {@link ClientExceptionWithBody} with appropriate HTTP status and error code.</p>
     */
    public ExceptionMap() {
        exceptions.put(CitizenConstants.ExceptionName.CITIZEN_NOT_ONBOARDED, message ->
            new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                CitizenConstants.ExceptionCode.CITIZEN_NOT_ONBOARDED,
                message
            )
        );

        exceptions.put(CitizenConstants.ExceptionName.TPP_NOT_FOUND, message ->
            new ClientExceptionWithBody(
                HttpStatus.NOT_FOUND,
                CitizenConstants.ExceptionCode.TPP_NOT_FOUND,
                message
            )
        );
    }

    /**
     * <p>Retrieves (does not throw) the exception instance associated with the provided key.</p>
     *
     * <p>If the key is missing, logs an error and returns a generic {@link RuntimeException}.</p>
     *
     * @param exceptionKey symbolic exception key (see {@link CitizenConstants.ExceptionName})
     * @param message descriptive message to include in the exception body
     * @return {@link RuntimeException} instance (specific or generic if key not found)
     */
    public RuntimeException throwException(String exceptionKey, String message) {
        if (exceptions.containsKey(exceptionKey)) {
            return exceptions.get(exceptionKey).apply(message);
        } else {
            log.error("Exception Name Not Found: {}", exceptionKey);
            return new RuntimeException();
        }
    }
}
