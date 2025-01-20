package it.gov.pagopa.onboarding.citizen.configuration;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RedissonConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Value("${redis.password}")
    private String redisPassword;
    @Bean
    public RedissonReactiveClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl).setPassword(redisPassword);
        return Redisson.create(config).reactive();
    }
}
