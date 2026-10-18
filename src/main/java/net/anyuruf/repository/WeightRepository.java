package net.anyuruf.repository;

import net.anyuruf.domain.Weight;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Weight entity.
 */
@SuppressWarnings("unused")
@Repository
public interface WeightRepository extends ReactiveCrudRepository<Weight, Long>, WeightRepositoryInternal {
    Flux<Weight> findAllBy(Pageable pageable);

    @Override
    Mono<Weight> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Weight> findAllWithEagerRelationships();

    @Override
    Flux<Weight> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM weight entity WHERE entity.user_id = :id")
    Flux<Weight> findByUser(Long id);

    @Query("SELECT * FROM weight entity WHERE entity.user_id IS NULL")
    Flux<Weight> findAllWhereUserIsNull();

    @Override
    <S extends Weight> Mono<S> save(S entity);

    @Override
    Flux<Weight> findAll();

    @Override
    Mono<Weight> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface WeightRepositoryInternal {
    Flux<Weight> findAllBy(Pageable pageable);

    Flux<Weight> findAll();

    Mono<Weight> findById(Long id);

    Mono<Weight> findOneWithEagerRelationships(Long id);

    Flux<Weight> findAllWithEagerRelationships();

    Flux<Weight> findAllWithEagerRelationships(Pageable page);
}
