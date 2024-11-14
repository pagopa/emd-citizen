package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.faker.CitizenConsentFaker;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloomFilterServiceTest {

    @Mock
    private CitizenRepository citizenRepository;

    @InjectMocks
    private BloomFilterServiceImpl bloomFilterService;

    @BeforeEach
    void setUp() {

        when(citizenRepository.findAll()).thenReturn(Flux.just(CitizenConsentFaker.mockInstance(true)));
        bloomFilterService.initializeBloomFilter();
    }

    @Test
    void testMightContain() {
        assertTrue(bloomFilterService.mightContain("fiscalCode"));
        assertFalse(bloomFilterService.mightContain("nonExistentFiscalCode"));
    }

    @Test
    void testUpdate() {
        when(citizenRepository.findAll()).thenReturn(Flux.just(CitizenConsentFaker.mockInstance(true)));
        bloomFilterService.update();
        assertTrue(bloomFilterService.mightContain("fiscalCode"));
        assertFalse(bloomFilterService.mightContain("nonexistentHashedFiscalCode"));
    }

    @Test
    void testAdd() {
        bloomFilterService.add("fiscalCode3");
        assertTrue(bloomFilterService.mightContain("fiscalCode3"));
    }
}
