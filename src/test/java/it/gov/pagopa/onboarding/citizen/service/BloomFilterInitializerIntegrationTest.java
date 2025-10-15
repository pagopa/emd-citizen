package it.gov.pagopa.onboarding.citizen.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "app.bloomFilter.expectedInsertions=12345",
    "app.bloomFilter.falseProbability=0.01"
})
class BloomFilterInitializerIntegrationTest {

  @Autowired
  private BloomFilterInitializer bloomFilterInitializer;

  @Test
  void testExpectedInsertionsInjected() {
    assertThat(bloomFilterInitializer.getExpectedInsertions()).isEqualTo(12345L);
  }

  @Test
  void testFalseProbabilityInjected() {
    assertThat(bloomFilterInitializer.getFalseProbability()).isEqualTo(0.01);
  }
}
