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

        public static final String COMPLETE_FISCAL_CODE = "^[A-Za-z]{6}[0-9]{2}[A-Za-z][0-9]{2}[A-Za-z][0-9]{3}[A-Za-z]$";
        public static final String FISCAL_CODE_STRUCTURE_REGEX = "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$";
        private ValidationRegex() {}
    }


    private CitizenConstants() {}
}
