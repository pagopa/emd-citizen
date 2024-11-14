package it.gov.pagopa.onboarding.citizen.service;


import com.azure.cosmos.implementation.guava25.hash.BloomFilter;
import com.azure.cosmos.implementation.guava25.hash.Funnels;
import it.gov.pagopa.onboarding.citizen.repository.CitizenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class BloomFilterServiceImpl implements BloomFilterService{

    private final CitizenRepository citizenRepository;

    private BloomFilter<String> bloomFilter;

    public BloomFilterServiceImpl(CitizenRepository citizenRepository) {
        this.citizenRepository = citizenRepository;
    }


    @PostConstruct
    public void initializeBloomFilter() {
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000, 0.01);

        citizenRepository.findAll()
                    .doOnNext(citizenConsent ->
                        bloomFilter.put(citizenConsent.getFiscalCode()))
                    .doOnComplete(() -> log.info("Bloom filter initialized"))
                    .subscribe();
    }
    @Override

    public boolean mightContain(String fiscalCode) {
        return bloomFilter.mightContain(fiscalCode);
    }


    @Scheduled(fixedRate = 3600000)
    public void update() {
        this.initializeBloomFilter();
    }

    public void add(String fiscalCode){
        bloomFilter.put(fiscalCode);
    }
}
