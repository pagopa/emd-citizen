package it.gov.pagopa.onboarding.citizen.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.redisson.api.RBloomFilterReactive;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BloomFilterServiceImplTest {
    @Mock
    private BloomFilterInitializer bloomFilterInitializer;

    @Mock
    private RBloomFilterReactive<String> bloomFilter;

    private BloomFilterServiceImpl bloomFilterService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(bloomFilterInitializer.getBloomFilter()).thenReturn(bloomFilter);
        bloomFilterService = new BloomFilterServiceImpl(bloomFilterInitializer);
    }

    @Test
    void testAdd() {
        when(bloomFilter.add(anyString())).thenReturn(Mono.just(true));
        bloomFilterService.add("12345");
        verify(bloomFilter).add("12345");
    }

    @Test
    void testAddFailure() {
        when(bloomFilter.add(anyString())).thenReturn(Mono.just(false));
        bloomFilterService.add("12345");
        verify(bloomFilter).add("12345");
    }

    @Test
    void testMightContainFound() {
        when(bloomFilter.contains(anyString())).thenReturn(Mono.just(true));
        Mono<String> result = bloomFilterService.mightContain("12345");
        StepVerifier.create(result)
                .expectNext("OK")
                .verifyComplete();
        verify(bloomFilter).contains("12345");
    }

    @Test
    void testMightContainNotFound() {
        when(bloomFilter.contains(anyString())).thenReturn(Mono.just(false));
        Mono<String> result = bloomFilterService.mightContain("12345");
        StepVerifier.create(result)
                .expectNext("NO CHANNELS ENABLED")
                .verifyComplete();
        verify(bloomFilter).contains("12345");
    }
}

