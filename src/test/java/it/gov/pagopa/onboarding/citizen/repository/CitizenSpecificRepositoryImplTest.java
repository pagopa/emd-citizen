package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenSpecificRepositoryImplTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private CitizenSpecificRepositoryImpl repository;

    @Test
    void testFindByFiscalCodeAndTppId() {
        String hashedFiscalCode = "hashedCode";
        String tppId = "tpp1";
        CitizenConsent citizenConsent = createMockCitizenConsent(hashedFiscalCode, tppId);

        when(mongoTemplate.aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        )).thenReturn(Flux.just(citizenConsent));

        StepVerifier.create(repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .expectNextMatches(result -> result.getFiscalCode().equals(hashedFiscalCode))
                .verifyComplete();

        Mockito.verify(mongoTemplate).aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        );
    }

    @Test
    void testFindByFiscalCodeWithAtLeastOneConsent() {
        String fiscalCode = "hashedCode";
        CitizenConsent citizenConsent = createMockCitizenConsent(fiscalCode, "tpp1");

        when(mongoTemplate.aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        )).thenReturn(Flux.just(citizenConsent));

        StepVerifier.create(repository.findByFiscalCodeWithAtLeastOneConsent(fiscalCode))
                .expectNextMatches(result -> result.getFiscalCode().equals(fiscalCode))
                .verifyComplete();

        Mockito.verify(mongoTemplate).aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        );
    }

    @Test
    void testFindByFiscalCodeWithAtLeastOneConsent_NoConsent() {
        String fiscalCode = "hashedCode";
        CitizenConsent citizenConsent = createMockCitizenNoConsent(fiscalCode, "tpp1");

        when(mongoTemplate.aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        )).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByFiscalCodeWithAtLeastOneConsent(fiscalCode))
                .expectComplete()
                .verify();

        Mockito.verify(mongoTemplate).aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        );
    }

    @Test
    void testFindByFiscalCodeAndTppId_TppIdNull() {
        String hashedFiscalCode = "hashedCode";
        String tppId = null;

        StepVerifier.create(repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .expectComplete() // Expect an empty Mono
                .verify();
    }

    @Test
    void testFindByFiscalCodeAndTppId_EmptyResult() {
        String hashedFiscalCode = "hashedCode";
        String tppId = "tpp1";

        when(mongoTemplate.aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        )).thenReturn(Flux.empty());

        StepVerifier.create(repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId))
                .expectComplete() // Expect an empty Mono
                .verify();

        Mockito.verify(mongoTemplate).aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        );
    }

    @Test
    void testConsentKeyWrapperGetterSetter() {
        CitizenSpecificRepositoryImpl.ConsentKeyWrapper consentKeyWrapper = new CitizenSpecificRepositoryImpl.ConsentKeyWrapper();

        consentKeyWrapper.setK("testKey");

        Assertions.assertEquals("testKey", consentKeyWrapper.getK());
    }

    @Test
    void testConsentKeyWrapperToString() {
        CitizenSpecificRepositoryImpl.ConsentKeyWrapper consentKeyWrapper = new CitizenSpecificRepositoryImpl.ConsentKeyWrapper();
        consentKeyWrapper.setK("testKey");

        Assertions.assertEquals("CitizenSpecificRepositoryImpl.ConsentKeyWrapper(k=testKey)", consentKeyWrapper.toString());
    }

    @Test
    void testConsentKeyWrapperEqualsAndHashCode() {
        CitizenSpecificRepositoryImpl.ConsentKeyWrapper consentKeyWrapper1 = new CitizenSpecificRepositoryImpl.ConsentKeyWrapper();
        CitizenSpecificRepositoryImpl.ConsentKeyWrapper consentKeyWrapper2 = new CitizenSpecificRepositoryImpl.ConsentKeyWrapper();

        consentKeyWrapper1.setK("testKey");
        consentKeyWrapper2.setK("testKey");

        Assertions.assertEquals(consentKeyWrapper1, consentKeyWrapper2);
        Assertions.assertEquals(consentKeyWrapper1.hashCode(), consentKeyWrapper2.hashCode());

        consentKeyWrapper2.setK("differentKey");
        Assertions.assertNotEquals(consentKeyWrapper1, consentKeyWrapper2);
    }

    private CitizenConsent createMockCitizenConsent(String hashedFiscalCode, String tppId) {
        CitizenConsent citizenConsent = new CitizenConsent();
        citizenConsent.setId("1");
        citizenConsent.setFiscalCode(hashedFiscalCode);

        Map<String, ConsentDetails> consents = new HashMap<>();
        ConsentDetails consentDetails = new ConsentDetails();
        consentDetails.setTppState(true);
        consents.put(tppId, consentDetails);
        citizenConsent.setConsents(consents);

        return citizenConsent;
    }

    private CitizenConsent createMockCitizenNoConsent(String hashedFiscalCode, String tppId) {
        CitizenConsent citizenConsent = new CitizenConsent();
        citizenConsent.setId("1");
        citizenConsent.setFiscalCode(hashedFiscalCode);

        Map<String, ConsentDetails> consents = new HashMap<>();
        ConsentDetails consentDetails = new ConsentDetails();
        consentDetails.setTppState(false);
        consents.put(tppId, consentDetails);
        citizenConsent.setConsents(consents);

        return citizenConsent;
    }

    @Test
    void testFindByTppIdEnabled_Success() {
        String tppId = "tpp1";
        CitizenConsent citizenConsent = createMockCitizenConsent("hashedCode", tppId);

        when(mongoTemplate.aggregate(
                Mockito.any(Aggregation.class),
                Mockito.eq("citizen_consents"),
                Mockito.eq(CitizenConsent.class)
        )).thenReturn(Flux.just(citizenConsent));

        StepVerifier.create(repository.findByTppIdEnabled(tppId))
                .expectNextMatches(result -> result.getConsents().containsKey(tppId) && result.getConsents().get(tppId).getTppState())
                .verifyComplete();

    }
}
