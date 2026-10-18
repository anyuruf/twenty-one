package net.anyuruf.repository;

import net.anyuruf.domain.Preferences;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Preferences entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PreferencesRepository extends ReactiveCrudRepository<Preferences, Long>, PreferencesRepositoryInternal {
    @Override
    Mono<Preferences> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Preferences> findAllWithEagerRelationships();

    @Override
    Flux<Preferences> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM preferences entity WHERE entity.user_id = :id")
    Flux<Preferences> findByUser(Long id);

    @Query("SELECT * FROM preferences entity WHERE entity.user_id IS NULL")
    Flux<Preferences> findAllWhereUserIsNull();

    @Override
    <S extends Preferences> Mono<S> save(S entity);

    @Override
    Flux<Preferences> findAll();

    @Override
    Mono<Preferences> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface PreferencesRepositoryInternal {
    Flux<Preferences> findAllBy(Pageable pageable);

    Flux<Preferences> findAll();

    Mono<Preferences> findById(Long id);

    Mono<Preferences> findOneWithEagerRelationships(Long id);

    Flux<Preferences> findAllWithEagerRelationships();

    Flux<Preferences> findAllWithEagerRelationships(Pageable page);
}
