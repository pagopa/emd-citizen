package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;


@ExtendWith({SpringExtension.class, MockitoExtension.class})
class BloomFilterInitializerTest {

    private static final String REDIS_LOCK_NAME = "startup-task-lock";
    private static final String REDDIS_BF_NAME = "emd-bloom-fiter";

    @Mock
    private RedissonReactiveClient redissonClient;

    @Mock
    private RLockReactive lock;

    @Mock
    private CitizenRepository citizenRepository;

    private BloomFilterInitializer bloomFilterInitializer;

    @Test
    void test(){
        MockitoAnnotations.openMocks(this);
        when(redissonClient.getLock(REDIS_LOCK_NAME)).thenReturn(lock);

        RBloomFilterReactive<?> bloomFilter = mock(RBloomFilterReactive.class);

        when(redissonClient.getBloomFilter(REDDIS_BF_NAME)).thenReturn((RBloomFilterReactive<Object>) bloomFilter);

        when(bloomFilter.tryInit(anyLong(), anyDouble())).thenReturn(Mono.empty());
        bloomFilterInitializer = new BloomFilterInitializer(redissonClient,citizenRepository);
        StepVerifier.create(bloomFilter.tryInit(1000L, 0.01))
                .verifyComplete();

        when(citizenRepository.findAll()).thenReturn(Flux.empty());
        StepVerifier.create(citizenRepository.findAll())
                .verifyComplete();

        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(Mono.just(true));

        bloomFilterInitializer.initializer();

        verify(lock).tryLock(0, 60, TimeUnit.SECONDS);

    }

    @Test
    void test2(){
        MockitoAnnotations.openMocks(this);
        when(redissonClient.getLock(REDIS_LOCK_NAME)).thenReturn(lock);

        RBloomFilterReactive<?> bloomFilter = mock(RBloomFilterReactive.class);

        when(redissonClient.getBloomFilter(REDDIS_BF_NAME)).thenReturn((RBloomFilterReactive<Object>) bloomFilter);

        when(bloomFilter.tryInit(anyLong(), anyDouble())).thenReturn(Mono.empty());
        bloomFilterInitializer = new BloomFilterInitializer(redissonClient,citizenRepository);
        StepVerifier.create(bloomFilter.tryInit(1000L, 0.01))
                .verifyComplete();

        when(citizenRepository.findAll()).thenReturn(Flux.empty());
        StepVerifier.create(citizenRepository.findAll())
                .verifyComplete();

        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class)))
                .thenReturn(Mono.just(false));

        bloomFilterInitializer.initializer();

        verify(lock).tryLock(0, 60, TimeUnit.SECONDS);

    }
}

