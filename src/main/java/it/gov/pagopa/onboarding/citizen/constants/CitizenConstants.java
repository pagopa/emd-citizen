package it.gov.pagopa.onboarding.citizen.constants;

public class CitizenConstants {
    public static final class ExceptionCode {

        public static final String CITIZEN_NOT_ONBOARDED = "CITIZEN_NOT_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_FOUND = "TPP_NOT_FOUND";

        private ExceptionCode() {}
    }

    public static final class ExceptionMessage {

        public static final String CITIZEN_NOT_ONBOARDED = "CITIZEN_NOT_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_FOUND = "TPP does not exist or is not active";

        private ExceptionMessage() {}
    }

    public static final class ExceptionName {

        public static final String CITIZEN_NOT_ONBOARDED = "CITIZEN_NOT_ONBOARDED";
        public static final String GENERIC_ERROR = "GENERIC_ERROR";
        public static final String TPP_NOT_FOUND = "TPP_NOT_FOUND";

        private ExceptionName() {}
    }

    public static final class ValidationRegex {

        private ValidationRegex() {}
    }


    private CitizenConstants() {}
}
