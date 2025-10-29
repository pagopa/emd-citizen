package it.gov.pagopa.onboarding.citizen.repository;

import it.gov.pagopa.onboarding.citizen.model.CitizenConsent;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * <p>Primary reactive repository interface for {@link CitizenConsent} persistence operations.</p>
 *
 * <p>Combines Spring Data MongoDB auto-generated CRUD methods with custom query implementations:</p>
 * <ul>
 *   <li>{@link ReactiveMongoRepository} provides standard operations (save, findById, delete, etc.)</li>
 *   <li>{@link CitizenSpecificRepository} exposes complex aggregation queries</li>
 * </ul>
 *
 * <p>Collection name: {@code citizen_consents}</p>
 *
 * @see CitizenSpecificRepository
 * @see CitizenSpecificRepositoryImpl
 */
public interface CitizenRepository extends ReactiveMongoRepository<CitizenConsent, String>, CitizenSpecificRepository {

    /**
     * <p>Finds a citizen consent document by fiscal code.</p>
     *
     * <p>Returns the entire document including all TPP consents.</p>
     *
     * @param fiscalCode citizen's fiscal code (must not be {@code null})
     * @return {@code Mono} emitting the document if found, empty otherwise
     */
    Mono<CitizenConsent> findByFiscalCode(String fiscalCode);
}
