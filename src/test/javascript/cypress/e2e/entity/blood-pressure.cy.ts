import {
  entityConfirmDeleteButtonSelector,
  entityCreateButtonSelector,
  entityCreateCancelButtonSelector,
  entityCreateSaveButtonSelector,
  entityDeleteButtonSelector,
  entityDetailsBackButtonSelector,
  entityDetailsButtonSelector,
  entityEditButtonSelector,
  entityTableSelector,
} from '../../support/entity';

describe('BloodPressure e2e test', () => {
  const bloodPressurePageUrl = '/blood-pressure';
  let username: string;
  let password: string;
  const bloodPressureSample = { timestamp: '2023-12-08T21:42:26.059Z', systolic: 2246, diastolic: 9192 };

  let bloodPressure;

  before(() => {
    cy.credentials().then(credentials => {
      ({ username, password } = credentials);
    });
  });

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/blood-pressures+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/blood-pressures').as('postEntityRequest');
    cy.intercept('DELETE', '/api/blood-pressures/*').as('deleteEntityRequest');
  });

  afterEach(() => {
    if (bloodPressure) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/blood-pressures/${bloodPressure.id}`,
      }).then(() => {
        bloodPressure = undefined;
      });
    }
  });

  it('BloodPressures menu should load BloodPressures page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('blood-pressure');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('BloodPressure').should('exist');
    cy.location('pathname').should('eq', bloodPressurePageUrl);
  });

  describe('BloodPressure page', () => {
    it('should have translated page title', () => {
      cy.visit(bloodPressurePageUrl);
      cy.getEntityHeading('BloodPressure').should('not.contain', 'healthPointsApp.bloodPressure.home.title');
    });

    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(bloodPressurePageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create BloodPressure page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.location('pathname').should('eq', `${bloodPressurePageUrl}/new`);
        cy.getEntityCreateUpdateHeading('BloodPressure');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', bloodPressurePageUrl);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/blood-pressures',
          body: bloodPressureSample,
        }).then(({ body }) => {
          bloodPressure = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/blood-pressures+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              headers: {
                link: '<http://localhost/api/blood-pressures?page=0&size=20>; rel="last",<http://localhost/api/blood-pressures?page=0&size=20>; rel="first"',
              },
              body: [bloodPressure],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(bloodPressurePageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details BloodPressure page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('bloodPressure');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', bloodPressurePageUrl);
      });

      it('edit button click should load edit BloodPressure page and go back', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('BloodPressure');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', bloodPressurePageUrl);
      });

      it('edit button click should load edit BloodPressure page and save', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('BloodPressure');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', bloodPressurePageUrl);
      });

      it('last delete button click should delete instance of BloodPressure', () => {
        cy.intercept('GET', '/api/blood-pressures/*').as('dialogDeleteRequest');
        cy.get(entityDeleteButtonSelector).last().click();
        cy.wait('@dialogDeleteRequest');
        cy.getEntityDeleteDialogHeading('bloodPressure').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', bloodPressurePageUrl);

        bloodPressure = undefined;
      });
    });
  });

  describe('new BloodPressure page', () => {
    beforeEach(() => {
      cy.visit(bloodPressurePageUrl);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('BloodPressure');
    });

    it('should create an instance of BloodPressure', () => {
      cy.get(`[data-cy="timestamp"]`).type('2023-12-09T02:24');
      cy.get(`[data-cy="timestamp"]`).blur();
      cy.get(`[data-cy="timestamp"]`).should('have.value', '2023-12-09T02:24');

      cy.get(`[data-cy="systolic"]`).type('11725');
      cy.get(`[data-cy="systolic"]`).should('have.value', '11725');

      cy.get(`[data-cy="diastolic"]`).type('20816');
      cy.get(`[data-cy="diastolic"]`).should('have.value', '20816');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        bloodPressure = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.location('pathname').should('eq', bloodPressurePageUrl);
    });
  });
});
