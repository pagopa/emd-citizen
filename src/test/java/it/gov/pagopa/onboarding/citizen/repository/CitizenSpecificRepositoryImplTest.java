package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitizenSpecificRepositoryImplTest {

    private ReactiveMongoTemplate mongoTemplate;
    private CitizenSpecificRepositoryImpl repository;

    @BeforeEach
    public void setUp() {
        mongoTemplate = Mockito.mock(ReactiveMongoTemplate.class);
        repository = new CitizenSpecificRepositoryImpl(mongoTemplate);
    }

    @Test
    void testFindByFiscalCodeAndTppId() {
        String hashedFiscalCode = "hashedCode";
        String tppId = "tpp1";
        CitizenConsent citizenConsent = new CitizenConsent();
        citizenConsent.setId("1");
        citizenConsent.setFiscalCode(hashedFiscalCode);

        Map<String, ConsentDetails> consents = new HashMap<>();
        ConsentDetails consentDetails = new ConsentDetails();
        consentDetails.setTppState(true);
        consents.put(tppId, consentDetails);
        citizenConsent.setConsents(consents);

        when(mongoTemplate.aggregate(Mockito.any(), Mockito.eq(CitizenConsent.class), Mockito.eq(CitizenConsent.class)))
                .thenReturn(Flux.just(citizenConsent));

        Mono<CitizenConsent> result = repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId);

        Assertions.assertEquals(hashedFiscalCode, Objects.requireNonNull(result.block()).getFiscalCode());
        Mockito.verify(mongoTemplate).aggregate(Mockito.any(), Mockito.eq(CitizenConsent.class), Mockito.eq(CitizenConsent.class));
    }

    @Test
    void testFindByFiscalCodeAndTppId_TppIdNull() {
        String hashedFiscalCode = "hashedCode";
        String tppId = null;
        Mono<CitizenConsent> result = repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId);

        Assertions.assertNotEquals(Boolean.TRUE, result.hasElement().block());
    }

    @Test
    void testFindByFiscalCodeAndTppId_EmptyResult() {
        String hashedFiscalCode = "hashedCode";
        String tppId = "tpp1";

        when(mongoTemplate.aggregate(Mockito.any(), Mockito.eq(CitizenConsent.class), Mockito.eq(CitizenConsent.class)))
                .thenReturn(Flux.empty());

        Mono<CitizenConsent> result = repository.findByFiscalCodeAndTppId(hashedFiscalCode, tppId);

        Assertions.assertNull(result.block());
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
}
