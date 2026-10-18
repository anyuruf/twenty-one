package net.anyuruf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.anyuruf.config.AsyncSyncConfiguration;
import net.anyuruf.config.DatabaseTestcontainer;
import net.anyuruf.config.ElasticsearchTestConfiguration;
import net.anyuruf.config.ElasticsearchTestContainer;
import net.anyuruf.config.TestSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        HealthPointsApp.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        DatabaseTestcontainer.class,
        ElasticsearchTestContainer.class,
        ElasticsearchTestConfiguration.class,
    }
)
public @interface IntegrationTest {
    // 5s is Spring's default https://github.com/spring-projects/spring-framework/blob/main/spring-test/src/main/java/org/springframework/test/web/reactive/server/DefaultWebTestClient.java#L106
    String DEFAULT_TIMEOUT = "PT5S";
    String DEFAULT_ENTITY_TIMEOUT = "PT5S";
}
