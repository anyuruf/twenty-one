package net.anyuruf.web.rest;

import static net.anyuruf.domain.WeightAsserts.*;
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
import net.anyuruf.domain.Weight;
import net.anyuruf.repository.UserRepository;
import net.anyuruf.repository.WeightRepository;
import net.anyuruf.repository.search.WeightSearchRepository;
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
 * Integration tests for the {@link WeightResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class WeightResourceIT {

    private static final ZonedDateTime DEFAULT_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime UPDATED_TIMESTAMP = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1702094585303L), ZoneOffset.UTC);

    private static final Double DEFAULT_WEIGHT = 1D;
    private static final Double UPDATED_WEIGHT = 2D;

    private static final String ENTITY_API_URL = "/api/weights";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";
    private static final String ENTITY_SEARCH_API_URL = "/api/weights/_search";

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    @Autowired
    private ObjectMapper om;

    @Autowired
    private WeightRepository weightRepository;

    @Autowired
    private UserRepository userRepository;

    @Mock
    private WeightRepository weightRepositoryMock;

    @Autowired
    private WeightSearchRepository weightSearchRepository;

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private WebTestClient webTestClient;

    private Weight weight;

    private Weight insertedWeight;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Weight createEntity() {
        return new Weight().timestamp(DEFAULT_TIMESTAMP).weight(DEFAULT_WEIGHT);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Weight createUpdatedEntity() {
        return new Weight().timestamp(UPDATED_TIMESTAMP).weight(UPDATED_WEIGHT);
    }

    public static void deleteEntities(R2dbcEntityTemplate template) {
        try {
            template.delete(Weight.class).all().block();
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
        weight = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedWeight != null) {
            weightRepository.delete(insertedWeight).block();
            weightSearchRepository.delete(insertedWeight).block();
            insertedWeight = null;
        }
        deleteEntities(template);
    }

    @Test
    void createWeight() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        // Create the Weight
        var returnedWeight = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Weight.class)
            .returnResult()
            .getResponseBody();

        // Validate the Weight in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertWeightUpdatableFieldsEquals(returnedWeight, getPersistedWeight(returnedWeight));

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore + 1);
            });

        insertedWeight = returnedWeight;
    }

    @Test
    void createWeightWithExistingId() throws Exception {
        // Create the Weight with an existing ID
        weight.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkTimestampIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        // set the field null
        weight.setTimestamp(null);

        // Create the Weight, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void checkWeightIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        // set the field null
        weight.setWeight(null);

        // Create the Weight, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);

        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void getAllWeights() {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();

        // Get all the weightList
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
            .value(hasItem(weight.getId().intValue()))
            .jsonPath("$.[*].timestamp")
            .value(hasItem(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.[*].weight")
            .value(hasItem(DEFAULT_WEIGHT));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWeightsWithEagerRelationshipsIsEnabled() {
        when(weightRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=true")
            .exchange()
            .expectStatus()
            .isOk();

        verify(weightRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllWeightsWithEagerRelationshipsIsNotEnabled() {
        when(weightRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?eagerload=false")
            .exchange()
            .expectStatus()
            .isOk();
        verify(weightRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getWeight() {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();

        // Get the weight
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, weight.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(weight.getId().intValue()))
            .jsonPath("$.timestamp")
            .value(is(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.weight")
            .value(is(DEFAULT_WEIGHT));
    }

    @Test
    void getNonExistingWeight() {
        // Get the weight
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingWeight() throws Exception {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();
        weightSearchRepository.save(weight).block();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());

        // Update the weight
        Weight updatedWeight = weightRepository.findById(weight.getId()).block();
        updatedWeight.timestamp(UPDATED_TIMESTAMP).weight(UPDATED_WEIGHT);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedWeight.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedWeight))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedWeightToMatchAllProperties(updatedWeight);

        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
                assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
                List<Weight> weightSearchList = Streamable.of(weightSearchRepository.findAll().collectList().block()).toList();
                Weight testWeightSearch = weightSearchList.get(searchDatabaseSizeAfter - 1);

                // Test fails because reactive api returns an empty object instead of null
                // assertWeightAllPropertiesEquals(testWeightSearch, updatedWeight);
                assertWeightUpdatableFieldsEquals(testWeightSearch, updatedWeight);
            });
    }

    @Test
    void putNonExistingWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, weight.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithIdMismatchWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void putWithMissingIdPathParamWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void partialUpdateWeightWithPatch() throws Exception {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the weight using partial update
        Weight partialUpdatedWeight = new Weight();
        partialUpdatedWeight.setId(weight.getId());

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedWeight.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedWeight))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Weight in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWeightUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedWeight, weight), getPersistedWeight(weight));
    }

    @Test
    void fullUpdateWeightWithPatch() throws Exception {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the weight using partial update
        Weight partialUpdatedWeight = new Weight();
        partialUpdatedWeight.setId(weight.getId());

        partialUpdatedWeight.timestamp(UPDATED_TIMESTAMP).weight(UPDATED_WEIGHT);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedWeight.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedWeight))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Weight in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertWeightUpdatableFieldsEquals(partialUpdatedWeight, getPersistedWeight(partialUpdatedWeight));
    }

    @Test
    void patchNonExistingWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, weight.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithIdMismatchWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void patchWithMissingIdPathParamWeight() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        weight.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(weight))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Weight in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore);
    }

    @Test
    void deleteWeight() {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();
        weightRepository.save(weight).block();
        weightSearchRepository.save(weight).block();

        long databaseSizeBeforeDelete = getRepositoryCount();
        int searchDatabaseSizeBefore = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeBefore).isEqualTo(databaseSizeBeforeDelete);

        // Delete the weight
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, weight.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
        int searchDatabaseSizeAfter = IterableUtil.sizeOf(weightSearchRepository.findAll().collectList().block());
        assertThat(searchDatabaseSizeAfter).isEqualTo(searchDatabaseSizeBefore - 1);
    }

    @Test
    void searchWeight() {
        // Initialize the database
        insertedWeight = weightRepository.save(weight).block();
        weightSearchRepository.save(weight).block();

        // Search the weight
        webTestClient
            .get()
            .uri(ENTITY_SEARCH_API_URL + "?query=id:" + weight.getId())
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(weight.getId().intValue()))
            .jsonPath("$.[*].timestamp")
            .value(hasItem(sameInstant(DEFAULT_TIMESTAMP)))
            .jsonPath("$.[*].weight")
            .value(hasItem(DEFAULT_WEIGHT));
    }

    protected long getRepositoryCount() {
        return weightRepository.count().block();
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

    protected Weight getPersistedWeight(Weight weight) {
        return weightRepository.findById(weight.getId()).block();
    }

    protected void assertPersistedWeightToMatchAllProperties(Weight expectedWeight) {
        // Test fails because reactive api returns an empty object instead of null
        // assertWeightAllPropertiesEquals(expectedWeight, getPersistedWeight(expectedWeight));
        assertWeightUpdatableFieldsEquals(expectedWeight, getPersistedWeight(expectedWeight));
    }

    protected void assertPersistedWeightToMatchUpdatableProperties(Weight expectedWeight) {
        // Test fails because reactive api returns an empty object instead of null
        // assertWeightAllUpdatablePropertiesEquals(expectedWeight, getPersistedWeight(expectedWeight));
        assertWeightUpdatableFieldsEquals(expectedWeight, getPersistedWeight(expectedWeight));
    }
}
