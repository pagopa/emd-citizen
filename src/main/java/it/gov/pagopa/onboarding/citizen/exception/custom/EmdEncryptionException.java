package it.gov.pagopa.onboarding.citizen.exception.custom;

import it.gov.pagopa.common.web.exception.ServiceException;
import it.gov.pagopa.onboarding.citizen.constants.OnboardingCitizenConstants.ExceptionCode;


public class EmdEncryptionException extends ServiceException {

  public EmdEncryptionException(String message, boolean printStackTrace, Throwable ex) {
    this(ExceptionCode.GENERIC_ERROR, message, printStackTrace, ex);
  }
  public EmdEncryptionException(String code, String message, boolean printStackTrace, Throwable ex) {
    super(code, message,null, printStackTrace, ex);
  }
}
