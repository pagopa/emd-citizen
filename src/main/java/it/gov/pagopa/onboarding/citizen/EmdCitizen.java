package it.gov.pagopa.onboarding.citizen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class EmdCitizen {

	public static void main(String[] args) {
		SpringApplication.run(EmdCitizen.class, args);
	}

}
