package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import it.gov.pagopa.onboarding.citizen.model.ConsentDetails;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@SuppressWarnings({"unchecked", "rawtypes"})
class BloomFilterInitializerTest {

    private static final String REDDIS_BF_NAME = "emd-bloom-filter";
    private static final String REDIS_LOCK_NAME = "startup-task-lock";


    private RBloomFilterReactive bloomFilter;

    @Mock
    private RLockReactive lock;

    @MockBean
    private RedissonReactiveClient redissonClient;

    @MockBean
    private CitizenRepository citizenRepository;

    private BloomFilterInitializer bloomFilterInitializer;

    @BeforeEach
    void setUp() {
        bloomFilter = mock(RBloomFilterReactive.class);
        MockitoAnnotations.openMocks(this);

        when(redissonClient.getBloomFilter(REDDIS_BF_NAME)).thenReturn(bloomFilter);
        when(redissonClient.getLock(REDIS_LOCK_NAME)).thenReturn(lock);
        when(lock.tryLock(0, 60, TimeUnit.SECONDS)).thenReturn(Mono.just(true));
        bloomFilterInitializer = new BloomFilterInitializer(redissonClient, citizenRepository);
    }

    private void mockCommonDependencies() {
        when(bloomFilter.contains("fiscalCode")).thenReturn(Mono.just(true));
        when(lock.tryLock(0, 60, TimeUnit.SECONDS)).thenReturn(Mono.just(true));

        Map<String, ConsentDetails> consents = new HashMap<>();
        consents.put("test", ConsentDetails.builder().tppState(true).build());

        when(bloomFilter.isExists()).thenReturn(Mono.just(true));
        when(citizenRepository.findAll()).thenReturn(Flux.just(
                CitizenConsent.builder()
                        .id("id")
                        .fiscalCode("fiscalCode")
                        .consents(consents)
                        .build()
        ));
        when(bloomFilter.add(anyString())).thenReturn(Mono.just(true));
    }

    @Test
    void testInitialize() {
        mockCommonDependencies();

        bloomFilterInitializer.initialize();

        StepVerifier.create(bloomFilter.contains("fiscalCode"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testResetBloomFilter() {
        mockCommonDependencies();
        when(bloomFilter.delete()).thenReturn(Mono.empty());
        when(bloomFilter.tryInit(1000L, 0.01)).thenReturn(Mono.just(true));

        bloomFilterInitializer.resetBloomFilter();

        StepVerifier.create(bloomFilter.contains("fiscalCode"))
                .expectNext(true)
                .verifyComplete();
    }
}



