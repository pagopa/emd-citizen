package it.gov.pagopa.common.configuration;

import com.mongodb.MongoException;
import reactor.util.retry.Retry;

import java.time.Duration;

/** Retry a single Cosmos read rather than replaying the whole Kafka message. */
public final class MongoRetrySpecs {
    private MongoRetrySpecs() {}

    public static boolean isThrottled(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof MongoException mongoException && mongoException.getCode() == 16500) {
                return true;
            }
        }
        return false;
    }

    public static Retry cosmosDbThrottling() {
        return Retry.backoff(3, Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(1))
                .jitter(0.5)
                .filter(MongoRetrySpecs::isThrottled)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }
}
