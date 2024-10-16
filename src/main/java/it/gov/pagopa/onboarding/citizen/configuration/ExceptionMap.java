package it.gov.pagopa.onboarding.citizen.configuration;


import it.gov.pagopa.common.web.exception.ClientExceptionWithBody;
import it.gov.pagopa.onboarding.citizen.constants.OnboardingCitizenConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Configuration
public class ExceptionMap {

    private final Map<String, Supplier<RuntimeException>> exMap = new HashMap<>();

    public ExceptionMap() {
        exMap.put(OnboardingCitizenConstants.ExceptionName.CITIZEN_NOT_ONBOARDED, () ->
                new ClientExceptionWithBody(
                        HttpStatus.NOT_FOUND,
                        OnboardingCitizenConstants.ExceptionCode.CITIZEN_NOT_ONBOARDED,
                        OnboardingCitizenConstants.ExceptionMessage.CITIZEN_NOT_ONBOARDED
                )
        );

    }

    public RuntimeException getException(String exceptionKey) {
        if (exMap.containsKey(exceptionKey)) {
            return exMap.get(exceptionKey).get();
        } else {
            throw new IllegalArgumentException(String.format("Exception Name Not Found: %s", exceptionKey));
        }
    }

}
