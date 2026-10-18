package net.anyuruf.repository;

import net.anyuruf.domain.Points;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Points entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PointsRepository extends ReactiveCrudRepository<Points, Long>, PointsRepositoryInternal {
    Flux<Points> findAllBy(Pageable pageable);

    @Override
    Mono<Points> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Points> findAllWithEagerRelationships();

    @Override
    Flux<Points> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM points entity WHERE entity.user_id = :id")
    Flux<Points> findByUser(Long id);

    @Query("SELECT * FROM points entity WHERE entity.user_id IS NULL")
    Flux<Points> findAllWhereUserIsNull();

    @Override
    <S extends Points> Mono<S> save(S entity);

    @Override
    Flux<Points> findAll();

    @Override
    Mono<Points> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface PointsRepositoryInternal {
    Flux<Points> findAllBy(Pageable pageable);

    Flux<Points> findAll();

    Mono<Points> findById(Long id);

    Mono<Points> findOneWithEagerRelationships(Long id);

    Flux<Points> findAllWithEagerRelationships();

    Flux<Points> findAllWithEagerRelationships(Pageable page);
}
