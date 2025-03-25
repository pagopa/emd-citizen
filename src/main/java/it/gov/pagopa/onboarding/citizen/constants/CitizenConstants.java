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

        public static final String FISCAL_CODE_STRUCTURE_REGEX = "(^([A-Za-z]{6}[0-9lmnpqrstuvLMNPQRSTUV]{2}[abcdehlmprstABCDEHLMPRST][0-9lmnpqrstuvLMNPQRSTUV]{2}[A-Za-z][0-9lmnpqrstuvLMNPQRSTUV]{3}[A-Za-z])$)|(^(\\d{11})$)";

        public static final String TPP_STRUCTURE_REGEX = "(^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}-\\d{13}$)";
        private ValidationRegex() {}
    }


    private CitizenConstants() {}
}
