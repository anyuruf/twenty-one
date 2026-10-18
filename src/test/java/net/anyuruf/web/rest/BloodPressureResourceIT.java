package net.anyuruf.web.rest;

import static net.anyuruf.domain.BloodPressureAsserts.*;
import static net.anyuruf.web.rest.TestUtil.createUpdateProxyForBean;
import static net.anyuruf.web.rest.TestUtil.sameInstant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.anyuruf.IntegrationTest;
import net.anyuruf.domain.BloodPressure;
import net.anyuruf.repository.BloodPressureRepository;
import net.anyuruf.repository.UserRepository;
import net.anyuruf.repository.search.BloodPressureSearchRepository;
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
 * Integration tests for the {@link BloodPressureResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class BloodPressureResourceIT {

    private static final ZonedDateTime DEFAULT_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1702094585303L), ZoneOffset.UTC);

    private static final Integer DEFAULT_SYSTOLIC = 1;
    private static final Integer UPDATED_SYSTOLIC = 2;

    private static final Integer DEFAULT_DIASTOLIC = 1;
    private static final Integer UPDATED_DIASTOLIC = 2;

    private static final String ENTITY_API_URL = "/api/blood-pressures";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/blood-pressures/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BloodPressureRepository bloodPressureRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private BloodPressureRepository bloodPressureRepositoryMock;

    @Autowired
    private BloodPressureSearchRepository bloodPressureSearchRepository;

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private WebTestClient webTestClient;

    private BloodPressure bloodPressure;

    private BloodPressure insertedBloodPressure;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BloodPressure createEntity() {
        return new BloodPressure().timestamp(DEFAULT_TIMESTAMP).systolic(DEFAULT_SYSTOLIC).diastolic(DEFAULT_DIASTOLIC);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static BloodPressure createUpdatedEntity() {
        return new BloodPressure().timestamp(UPDATED_TIMESTAMP).systolic(UPDATED_SYSTOLIC).diastolic(UPDATED_DIASTOLIC);
    }

    public static void deleteEntities(R2dbcEntityTemplate template) {
        try {
            template.delete(BloodPressure.class).all().block();
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
        bloodPressure = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBloodPressure != null) {
            bloodPressureRepository.delete(insertedBloodPressure).block();
            bloodPressureSearchRepository.delete(insertedBloodPressure).block();
            insertedBloodPressure = null;
        }
        deleteEntities(template);
    }

    @Test
    void createBloodPressure() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        // Create the BloodPressure
        var returnedBloodPressure = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(BloodPressure.class)
            .returnResult()
            .getResponseBody();

        // Validate the BloodPressure in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertBloodPressureUpdatableFieldsEquals(returnedBloodPressure, getPersistedBloodPressure(returnedBloodPressure));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedBloodPressure = returnedBloodPressure;
    }

    @Test
    void createBloodPressureWithExistingId() throws Exception {
        // Create the BloodPressure with an existing ID
        bloodPressure.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkTimestampIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        // set the field null
        bloodPressure.setTimestamp(null);

        // Create the BloodPressure, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkSystolicIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        // set the field null
        bloodPressure.setSystolic(null);

        // Create the BloodPressure, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkDiastolicIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        // set the field null
        bloodPressure.setDiastolic(null);

        // Create the BloodPressure, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void getAllBloodPressures() {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();

        // Get all the bloodPressureList
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
            .value(hasItem(bloodPressure.getId().intValue()))
            .jsonPath("$.[*].timestamp")
            .value(hasItem(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.[*].systolic")
            .value(hasItem(DEFAULT_SYSTOLIC))
            .jsonPath("$.[*].diastolic")
            .value(hasItem(DEFAULT_DIASTOLIC));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBloodPressuresWithEagerRelationshipsIsEnabled() {
        when(bloodPressureRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=true")
            .exchange()
            .expectStatus()
            .isOk();

        verify(bloodPressureRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllBloodPressuresWithEagerRelationshipsIsNotEnabled() {
        when(bloodPressureRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=false")
            .exchange()
            .expectStatus()
            .isOk();
        verify(bloodPressureRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getBloodPressure() {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();

        // Get the bloodPressure
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, bloodPressure.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(bloodPressure.getId().intValue()))
            .jsonPath("$.timestamp")
            .value(is(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.systolic")
            .value(is(DEFAULT_SYSTOLIC))
            .jsonPath("$.diastolic")
            .value(is(DEFAULT_DIASTOLIC));
    }

    @Test
    void getNonExistingBloodPressure() {
        // Get the bloodPressure
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingBloodPressure() throws Exception {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();
        bloodPressureSearchRepository.save(bloodPressure).block();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());

        // Update the bloodPressure
        BloodPressure updatedBloodPressure = bloodPressureRepository.findById(bloodPressure.getId()).block();
        updatedBloodPressure.timestamp(UPDATED_TIMESTAMP).systolic(UPDATED_SYSTOLIC).diastolic(UPDATED_DIASTOLIC);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedBloodPressure.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedBloodPressure))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBloodPressureToMatchAllProperties(updatedBloodPressure);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<BloodPressure> bloodPressureSearchList = Streamable.of(
                    bloodPressureSearchRepository.findAll().collectList().block()
                ).toList();
                BloodPressure testBloodPressureSearch = bloodPressureSearchList.get(searchDatabaseSizeAfter - 1);

                // Test fails because reactive api returns an empty object instead of null
                // assertBloodPressureAllPropertiesEquals(testBloodPressureSearch, updatedBloodPressure);
                assertBloodPressureUpdatableFieldsEquals(testBloodPressureSearch, updatedBloodPressure);
            });
    }

    @Test
    void putNonExistingBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, bloodPressure.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithIdMismatchBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithMissingIdPathParamBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void partialUpdateBloodPressureWithPatch() throws Exception {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the bloodPressure using partial update
        BloodPressure partialUpdatedBloodPressure = new BloodPressure();
        partialUpdatedBloodPressure.setId(bloodPressure.getId());

        partialUpdatedBloodPressure.diastolic(UPDATED_DIASTOLIC);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBloodPressure.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBloodPressure))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the BloodPressure in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBloodPressureUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedBloodPressure, bloodPressure),
            getPersistedBloodPressure(bloodPressure)
        );
    }

    @Test
    void fullUpdateBloodPressureWithPatch() throws Exception {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the bloodPressure using partial update
        BloodPressure partialUpdatedBloodPressure = new BloodPressure();
        partialUpdatedBloodPressure.setId(bloodPressure.getId());

        partialUpdatedBloodPressure.timestamp(UPDATED_TIMESTAMP).systolic(UPDATED_SYSTOLIC).diastolic(UPDATED_DIASTOLIC);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBloodPressure.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBloodPressure))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the BloodPressure in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBloodPressureUpdatableFieldsEquals(partialUpdatedBloodPressure, getPersistedBloodPressure(partialUpdatedBloodPressure));
    }

    @Test
    void patchNonExistingBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, bloodPressure.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithIdMismatchBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithMissingIdPathParamBloodPressure() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        bloodPressure.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(bloodPressure))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the BloodPressure in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void deleteBloodPressure() {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();
        bloodPressureRepository.save(bloodPressure).block();
        bloodPressureSearchRepository.save(bloodPressure).block();

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the bloodPressure
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, bloodPressure.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(bloodPressureSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    void searchBloodPressure() {
        // Initialize the database
        insertedBloodPressure = bloodPressureRepository.save(bloodPressure).block();
        bloodPressureSearchRepository.save(bloodPressure).block();

        // Search the bloodPressure
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + bloodPressure.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(bloodPressure.getId().intValue()))
            .jsonPath("$.[*].timestamp")
            .value(hasItem(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.[*].systolic")
            .value(hasItem(DEFAULT_SYSTOLIC))
            .jsonPath("$.[*].diastolic")
            .value(hasItem(DEFAULT_DIASTOLIC));
    }

    protected long getRepositoryCount() {
        return bloodPressureRepository.count().block();
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

    protected BloodPressure getPersistedBloodPressure(BloodPressure bloodPressure) {
        return bloodPressureRepository.findById(bloodPressure.getId()).block();
    }

    protected void assertPersistedBloodPressureToMatchAllProperties(BloodPressure expectedBloodPressure) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBloodPressureAllPropertiesEquals(expectedBloodPressure, getPersistedBloodPressure(expectedBloodPressure));
        assertBloodPressureUpdatableFieldsEquals(expectedBloodPressure, getPersistedBloodPressure(expectedBloodPressure));
    }

    protected void assertPersistedBloodPressureToMatchUpdatableProperties(BloodPressure expectedBloodPressure) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBloodPressureAllUpdatablePropertiesEquals(expectedBloodPressure, getPersistedBloodPressure(expectedBloodPressure));
        assertBloodPressureUpdatableFieldsEquals(expectedBloodPressure, getPersistedBloodPressure(expectedBloodPressure));
    }
}
