package net.anyuruf.repository;

import java.util.List;
import java.util.Map;
import net.anyuruf.domain.Points;
import net.anyuruf.domain.User;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the Points entity.
 */
@SuppressWarnings("unused")
class PointsRepositoryInternalImpl extends AbstractR2dbcRepository<Points, Long> implements PointsRepositoryInternal {

    private static final String SELECT_WITH_RELATIONS_SQL =
        "SELECT e.* FROM points e" + " LEFT JOIN jhi_user e_user ON e.user_id = e_user.id";

    // The clients sort by the related entity display field, with the entity id as secondary sort.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
        Map.entry("id", "e.id"),
        Map.entry("user.id", "e_user.id"),
        Map.entry("user.login", "e_user.login")
    );

    public PointsRepositoryInternalImpl(R2dbcEntityTemplate template, R2dbcConverter converter, R2dbcDialect dialect) {
        super(Points.class, template, converter, dialect);
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
    protected Mono<List<Points>> populateRelationships(List<Points> entities) {
        return Mono.just(entities).flatMap(populated -> populate(populated, Points::getUserId, User.class, User::getId, Points::setUser));
    }
}
