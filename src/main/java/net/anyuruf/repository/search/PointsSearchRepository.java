package net.anyuruf.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import net.anyuruf.domain.Points;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

/**
 * Spring Data Elasticsearch repository for the {@link Points} entity.
 */
public interface PointsSearchRepository extends ReactiveElasticsearchRepository<Points, Long>, PointsSearchRepositoryInternal {}

interface PointsSearchRepositoryInternal {
    Flux<Points> search(String query, Pageable pageable);

    Flux<Points> search(Query query);
}

class PointsSearchRepositoryInternalImpl implements PointsSearchRepositoryInternal {

    private final ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;

    PointsSearchRepositoryInternalImpl(ReactiveElasticsearchTemplate reactiveElasticsearchTemplate) {
        this.reactiveElasticsearchTemplate = reactiveElasticsearchTemplate;
    }

    @Override
    public Flux<Points> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        nativeQuery.setPageable(pageable);
        return search(nativeQuery);
    }

    @Override
    public Flux<Points> search(Query query) {
        return reactiveElasticsearchTemplate.search(query, Points.class).map(SearchHit::getContent);
    }
}
