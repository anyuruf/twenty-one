package net.anyuruf.repository.search;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import net.anyuruf.domain.BloodPressure;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

/**
 * Spring Data Elasticsearch repository for the {@link BloodPressure} entity.
 */
public interface BloodPressureSearchRepository
    extends ReactiveElasticsearchRepository<BloodPressure, Long>, BloodPressureSearchRepositoryInternal {}

interface BloodPressureSearchRepositoryInternal {
    Flux<BloodPressure> search(String query, Pageable pageable);

    Flux<BloodPressure> search(Query query);
}

class BloodPressureSearchRepositoryInternalImpl implements BloodPressureSearchRepositoryInternal {

    private final ReactiveElasticsearchTemplate reactiveElasticsearchTemplate;

    BloodPressureSearchRepositoryInternalImpl(ReactiveElasticsearchTemplate reactiveElasticsearchTemplate) {
        this.reactiveElasticsearchTemplate = reactiveElasticsearchTemplate;
    }

    @Override
    public Flux<BloodPressure> search(String query, Pageable pageable) {
        NativeQuery nativeQuery = new NativeQuery(QueryStringQuery.of(qs -> qs.query(query))._toQuery());
        nativeQuery.setPageable(pageable);
        return search(nativeQuery);
    }

    @Override
    public Flux<BloodPressure> search(Query query) {
        return reactiveElasticsearchTemplate.search(query, BloodPressure.class).map(SearchHit::getContent);
    }
}
