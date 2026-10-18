package net.anyuruf.repository;

import java.util.List;
import java.util.Map;
import net.anyuruf.domain.BloodPressure;
import net.anyuruf.domain.User;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the BloodPressure entity.
 */
@SuppressWarnings("unused")
class BloodPressureRepositoryInternalImpl extends AbstractR2dbcRepository<BloodPressure, Long> implements BloodPressureRepositoryInternal {

    private static final String SELECT_WITH_RELATIONS_SQL =
        "SELECT e.* FROM blood_pressure e" + " LEFT JOIN jhi_user e_user ON e.user_id = e_user.id";

    // The clients sort by the related entity display field, with the entity id as secondary sort.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
        Map.entry("id", "e.id"),
        Map.entry("user.id", "e_user.id"),
        Map.entry("user.login", "e_user.login")
    );

    public BloodPressureRepositoryInternalImpl(R2dbcEntityTemplate template, R2dbcConverter converter, R2dbcDialect dialect) {
        super(BloodPressure.class, template, converter, dialect);
    }

    @Override
    protected String selectWithRelationsSql() {
        return SELECT_WITH_RELATIONS_SQL;
    }

    @Override
    protected Map<String, String> sortColumns() {
        return SORT_COLUMNS;
    }

    @Override
    protected Mono<List<BloodPressure>> populateRelationships(List<BloodPressure> entities) {
        return Mono.just(entities).flatMap(populated ->
            populate(populated, BloodPressure::getUserId, User.class, User::getId, BloodPressure::setUser)
        );
    }
}
