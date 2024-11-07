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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
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
    void testFindByFiscalCodeAndTppStateTrue() {
        CitizenSpecificRepositoryImpl.ConsentKeyWrapper key = new CitizenSpecificRepositoryImpl.ConsentKeyWrapper();
        key.setKey("tppId");
        String hashedFiscalCode = "hashedCode";

        when(mongoTemplate.aggregate(Mockito.any(), anyString(), Mockito.eq(CitizenSpecificRepositoryImpl.ConsentKeyWrapper.class)))
                .thenReturn(Flux.just(key));

        List<String> result = repository.findByFiscalCodeAndTppStateTrue(hashedFiscalCode).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1,result.size());
        Assertions.assertEquals("tppId", result.get(0));

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

        Assertions.assertEquals(hashedFiscalCode, result.block().getFiscalCode());
        Mockito.verify(mongoTemplate).aggregate(Mockito.any(), Mockito.eq(CitizenConsent.class), Mockito.eq(CitizenConsent.class));
    }

}