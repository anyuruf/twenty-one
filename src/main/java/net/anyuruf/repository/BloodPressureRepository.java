package net.anyuruf.repository;

import net.anyuruf.domain.BloodPressure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the BloodPressure entity.
 */
@SuppressWarnings("unused")
@Repository
public interface BloodPressureRepository extends ReactiveCrudRepository<BloodPressure, Long>, BloodPressureRepositoryInternal {
    Flux<BloodPressure> findAllBy(Pageable pageable);

    @Override
    Mono<BloodPressure> findOneWithEagerRelationships(Long id);

    @Override
    Flux<BloodPressure> findAllWithEagerRelationships();

    @Override
    Flux<BloodPressure> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM blood_pressure entity WHERE entity.user_id = :id")
    Flux<BloodPressure> findByUser(Long id);

    @Query("SELECT * FROM blood_pressure entity WHERE entity.user_id IS NULL")
    Flux<BloodPressure> findAllWhereUserIsNull();

    @Override
    <S extends BloodPressure> Mono<S> save(S entity);

    @Override
    Flux<BloodPressure> findAll();

    @Override
    Mono<BloodPressure> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface BloodPressureRepositoryInternal {
    Flux<BloodPressure> findAllBy(Pageable pageable);

    Flux<BloodPressure> findAll();

    Mono<BloodPressure> findById(Long id);

    Mono<BloodPressure> findOneWithEagerRelationships(Long id);

    Flux<BloodPressure> findAllWithEagerRelationships();

    Flux<BloodPressure> findAllWithEagerRelationships(Pageable page);
}
