package net.anyuruf.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import net.anyuruf.domain.Weight;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

/**
 * Spring Data Elasticsearch repository for the {@link Weight} entity.
 */
public interface WeightSearchRepository extends ReactiveElasticsearchRepository<Weight, Long>, WeightSearchRepositoryInternal {}

interface WeightSearchRepositoryInternal {
    Flux<Weight> search(String query, Pageable pageable);

    Flux<Weight> search(Query query);
}

class WeightSearchRepositoryInternalImpl implements WeightSearchRepositoryInternal {

    private final ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;

    WeightSearchRepositoryInternalImpl(ReactiveElasticsearchTemplate reactiveElasticsearchTemplate) {
        this.reactiveElasticsearchTemplate = reactiveElasticsearchTemplate;
    }

    @Override
    public Flux<Weight> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        nativeQuery.setPageable(pageable);
        return search(nativeQuery);
    }

    @Override
    public Flux<Weight> search(Query query) {
        return reactiveElasticsearchTemplate.search(query, Weight.class).map(SearchHit::getContent);
    }
}
