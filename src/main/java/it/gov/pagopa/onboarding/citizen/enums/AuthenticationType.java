package it.gov.pagopa.onboarding.citizen.enums;

import lombok.Getter;

@Getter
public enum AuthenticationType {
    OAUTH2("OAUTH2");


    private final String status;

    AuthenticationType(String status) {
        this.status = status;
    }

}
