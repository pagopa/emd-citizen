package it.gov.pagopa.onboarding.citizen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdOnboardingCitizen {

	public static void main(String[] args) {
		SpringApplication.run(EmdOnboardingCitizen.class, args);
	}

}
