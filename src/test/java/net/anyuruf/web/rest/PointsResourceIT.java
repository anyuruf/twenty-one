package net.anyuruf.web.rest;

import static net.anyuruf.domain.PointsAsserts.*;
import static net.anyuruf.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.anyuruf.IntegrationTest;
import net.anyuruf.domain.Points;
import net.anyuruf.repository.PointsRepository;
import net.anyuruf.repository.UserRepository;
import net.anyuruf.repository.search.PointsSearchRepository;
import org.assertj.core.util.IterableUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for the {@link PointsResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class PointsResourceIT {

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATE = LocalDate.parse("2023-12-09");

    private static final Integer DEFAULT_EXERCISE = 1;
    private static final Integer UPDATED_EXERCISE = 2;

    private static final Integer DEFAULT_MEALS = 1;
    private static final Integer UPDATED_MEALS = 2;

    private static final Integer DEFAULT_ALCOHOL = 1;
    private static final Integer UPDATED_ALCOHOL = 2;

    private static final String DEFAULT_NOTES = "AAAAAAAAAA";
    private static final String UPDATED_NOTES = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/points";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/points/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private PointsRepository pointsRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private PointsRepository pointsRepositoryMock;

    @Autowired
    private PointsSearchRepository pointsSearchRepository;

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private WebTestClient webTestClient;

    private Points points;

    private Points insertedPoints;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Points createEntity() {
        return new Points()
            .date(DEFAULT_DATE)
            .exercise(DEFAULT_EXERCISE)
            .meals(DEFAULT_MEALS)
            .alcohol(DEFAULT_ALCOHOL)
            .notes(DEFAULT_NOTES);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Points createUpdatedEntity() {
        return new Points()
            .date(UPDATED_DATE)
            .exercise(UPDATED_EXERCISE)
            .meals(UPDATED_MEALS)
            .alcohol(UPDATED_ALCOHOL)
            .notes(UPDATED_NOTES);
    }

    public static void deleteEntities(R2dbcEntityTemplate template) {
        try {
            template.delete(Points.class).all().block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void setupCsrf() {
        webTestClient = webTestClient.mutateWith(csrf());
    }

    @BeforeEach
    void initTest() {
        points = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedPoints != null) {
            pointsRepository.delete(insertedPoints).block();
            pointsSearchRepository.delete(insertedPoints).block();
            insertedPoints = null;
        }
        deleteEntities(template);
    }

    @Test
    void createPoints() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        // Create the Points
        var returnedPoints = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Points.class)
            .returnResult()
            .getResponseBody();

        // Validate the Points in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertPointsUpdatableFieldsEquals(returnedPoints, getPersistedPoints(returnedPoints));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedPoints = returnedPoints;
    }

    @Test
    void createPointsWithExistingId() throws Exception {
        // Create the Points with an existing ID
        points.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        // set the field null
        points.setDate(null);

        // Create the Points, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void getAllPointses() {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();

        // Get all the pointsList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(points.getId().intValue()))
            .jsonPath("$.[*].date")
            .value(hasItem(DEFAULT_DATE.toString()))
            .jsonPath("$.[*].exercise")
            .value(hasItem(DEFAULT_EXERCISE))
            .jsonPath("$.[*].meals")
            .value(hasItem(DEFAULT_MEALS))
            .jsonPath("$.[*].alcohol")
            .value(hasItem(DEFAULT_ALCOHOL))
            .jsonPath("$.[*].notes")
            .value(hasItem(DEFAULT_NOTES));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPointsesWithEagerRelationshipsIsEnabled() {
        when(pointsRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=true")
            .exchange()
            .expectStatus()
            .isOk();

        verify(pointsRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllPointsesWithEagerRelationshipsIsNotEnabled() {
        when(pointsRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=false")
            .exchange()
            .expectStatus()
            .isOk();
        verify(pointsRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getPoints() {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();

        // Get the points
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, points.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(points.getId().intValue()))
            .jsonPath("$.date")
            .value(is(DEFAULT_DATE.toString()))
            .jsonPath("$.exercise")
            .value(is(DEFAULT_EXERCISE))
            .jsonPath("$.meals")
            .value(is(DEFAULT_MEALS))
            .jsonPath("$.alcohol")
            .value(is(DEFAULT_ALCOHOL))
            .jsonPath("$.notes")
            .value(is(DEFAULT_NOTES));
    }

    @Test
    void getNonExistingPoints() {
        // Get the points
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingPoints() throws Exception {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();
        pointsSearchRepository.save(points).block();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());

        // Update the points
        Points updatedPoints = pointsRepository.findById(points.getId()).block();
        updatedPoints.date(UPDATED_DATE).exercise(UPDATED_EXERCISE).meals(UPDATED_MEALS).alcohol(UPDATED_ALCOHOL).notes(UPDATED_NOTES);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedPoints.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedPoints))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedPointsToMatchAllProperties(updatedPoints);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Points> pointsSearchList = Streamable.of(pointsSearchRepository.findAll().collectList().block()).toList();
                Points testPointsSearch = pointsSearchList.get(searchDatabaseSizeAfter - 1);

                // Test fails because reactive api returns an empty object instead of null
                // assertPointsAllPropertiesEquals(testPointsSearch, updatedPoints);
                assertPointsUpdatableFieldsEquals(testPointsSearch, updatedPoints);
            });
    }

    @Test
    void putNonExistingPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, points.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithIdMismatchPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithMissingIdPathParamPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void partialUpdatePointsWithPatch() throws Exception {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the points using partial update
        Points partialUpdatedPoints = new Points();
        partialUpdatedPoints.setId(points.getId());

        partialUpdatedPoints.exercise(UPDATED_EXERCISE).meals(UPDATED_MEALS);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPoints.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedPoints))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Points in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPointsUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedPoints, points), getPersistedPoints(points));
    }

    @Test
    void fullUpdatePointsWithPatch() throws Exception {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the points using partial update
        Points partialUpdatedPoints = new Points();
        partialUpdatedPoints.setId(points.getId());

        partialUpdatedPoints
            .date(UPDATED_DATE)
            .exercise(UPDATED_EXERCISE)
            .meals(UPDATED_MEALS)
            .alcohol(UPDATED_ALCOHOL)
            .notes(UPDATED_NOTES);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedPoints.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedPoints))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Points in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPointsUpdatableFieldsEquals(partialUpdatedPoints, getPersistedPoints(partialUpdatedPoints));
    }

    @Test
    void patchNonExistingPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, points.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithIdMismatchPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithMissingIdPathParamPoints() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        points.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(points))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Points in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void deletePoints() {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();
        pointsRepository.save(points).block();
        pointsSearchRepository.save(points).block();

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the points
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, points.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(pointsSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    void searchPoints() {
        // Initialize the database
        insertedPoints = pointsRepository.save(points).block();
        pointsSearchRepository.save(points).block();

        // Search the points
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + points.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(points.getId().intValue()))
            .jsonPath("$.[*].date")
            .value(hasItem(DEFAULT_DATE.toString()))
            .jsonPath("$.[*].exercise")
            .value(hasItem(DEFAULT_EXERCISE))
            .jsonPath("$.[*].meals")
            .value(hasItem(DEFAULT_MEALS))
            .jsonPath("$.[*].alcohol")
            .value(hasItem(DEFAULT_ALCOHOL))
            .jsonPath("$.[*].notes")
            .value(hasItem(DEFAULT_NOTES));
    }

    protected long getRepositoryCount() {
        return pointsRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Points getPersistedPoints(Points points) {
        return pointsRepository.findById(points.getId()).block();
    }

    protected void assertPersistedPointsToMatchAllProperties(Points expectedPoints) {
        // Test fails because reactive api returns an empty object instead of null
        // assertPointsAllPropertiesEquals(expectedPoints, getPersistedPoints(expectedPoints));
        assertPointsUpdatableFieldsEquals(expectedPoints, getPersistedPoints(expectedPoints));
    }

    protected void assertPersistedPointsToMatchUpdatableProperties(Points expectedPoints) {
        // Test fails because reactive api returns an empty object instead of null
        // assertPointsAllUpdatablePropertiesEquals(expectedPoints, getPersistedPoints(expectedPoints));
        assertPointsUpdatableFieldsEquals(expectedPoints, getPersistedPoints(expectedPoints));
    }
}
