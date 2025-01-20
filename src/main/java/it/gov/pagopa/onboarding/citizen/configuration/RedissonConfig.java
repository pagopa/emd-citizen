package it.gov.pagopa.onboarding.citizen.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redis.url}")
    private String redisUrl;
    @Bean
    public RedissonReactiveClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);
        return Redisson.create(config).reactive();
    }
}
