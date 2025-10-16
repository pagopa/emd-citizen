// Java
package it.gov.pagopa.onboarding.citizen.service;

import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RBloomFilterReactive;
import org.redisson.api.RLockReactive;
import org.redisson.api.RedissonReactiveClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "app.bloomFilter.expectedInsertions=12345",
    "app.bloomFilter.falseProbability=0.01"
})
class BloomFilterPropertiesTest {

  @Value("${app.bloomFilter.expectedInsertions}")
  private long expectedInsertions;

  @Value("${app.bloomFilter.falseProbability}")
  private double falseProbability;

  @MockBean
  private BloomFilterInitializer bloomFilterInitializer;

  @Configuration
  static class RedissonReactiveClientConfig {

    @Bean
    public RedissonReactiveClient redissonClient() {

      final RedissonReactiveClient mock = Mockito.mock(RedissonReactiveClient.class);
      RBloomFilterReactive bloomFilter = Mockito.mock(RBloomFilterReactive.class);
      RLockReactive lockMock = Mockito.mock(RLockReactive.class);

      when(mock.getBloomFilter(Mockito.anyString())).thenReturn(bloomFilter);
      when(mock.getLock(Mockito.anyString())).thenReturn(lockMock);
      when(bloomFilter.isExists()).thenReturn(Mono.just(false));
      when(bloomFilter.tryInit(Mockito.anyLong(), Mockito.anyDouble())).thenReturn(reactor.core.publisher.Mono.just(true));
      when(lockMock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(Mono.just(true));
      return mock;

    }
  }

  @Configuration
  static class RLockReactiveConfig {

    @Bean
    public RLockReactive rLockReactive() {

      final RLockReactive mock = Mockito.mock(RLockReactive.class);
      when(mock.tryLock(Mockito.anyLong(), Mockito.anyLong(), Mockito.any())).thenReturn(
          Mono.just(true));
      return mock;

    }
  }

  @Configuration
  static class CitizenRepositoryConfig {

    @Bean
    public CitizenRepository citizenRepository() {

      final CitizenRepository mock = Mockito.mock(CitizenRepository.class);
      when(mock.findAll()).thenReturn(reactor.core.publisher.Flux.empty());
      return mock;

    }
  }

  @Test
  void testExpectedInsertionsInjected() {
    assertThat(expectedInsertions).isEqualTo(12345L);
  }

  @Test
  void testFalseProbabilityInjected() {
    assertThat(falseProbability).isEqualTo(0.01);
  }
}
