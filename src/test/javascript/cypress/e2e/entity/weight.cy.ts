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

describe('Weight e2e test', () => {
  const weightPageUrl = '/weight';
  let username: string;
  let password: string;
  const weightSample = { timestamp: '2023-12-08T17:50:18.133Z', weight: 15547.14 };

  let weight;

  before(() => {
    cy.credentials().then(credentials => {
      ({ username, password } = credentials);
    });
  });

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/weights+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/weights').as('postEntityRequest');
    cy.intercept('DELETE', '/api/weights/*').as('deleteEntityRequest');
  });

  afterEach(() => {
    if (weight) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/weights/${weight.id}`,
      }).then(() => {
        weight = undefined;
      });
    }
  });

  it('Weights menu should load Weights page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('weight');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('Weight').should('exist');
    cy.location('pathname').should('eq', weightPageUrl);
  });

  describe('Weight page', () => {
    it('should have translated page title', () => {
      cy.visit(weightPageUrl);
      cy.getEntityHeading('Weight').should('not.contain', 'healthPointsApp.weight.home.title');
    });

    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(weightPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create Weight page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.location('pathname').should('eq', `${weightPageUrl}/new`);
        cy.getEntityCreateUpdateHeading('Weight');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', weightPageUrl);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/weights',
          body: weightSample,
        }).then(({ body }) => {
          weight = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/weights+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              headers: {
                link: '<http://localhost/api/weights?page=0&size=20>; rel="last",<http://localhost/api/weights?page=0&size=20>; rel="first"',
              },
              body: [weight],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(weightPageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details Weight page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('weight');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', weightPageUrl);
      });

      it('edit button click should load edit Weight page and go back', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Weight');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', weightPageUrl);
      });

      it('edit button click should load edit Weight page and save', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Weight');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', weightPageUrl);
      });

      it('last delete button click should delete instance of Weight', () => {
        cy.intercept('GET', '/api/weights/*').as('dialogDeleteRequest');
        cy.get(entityDeleteButtonSelector).last().click();
        cy.wait('@dialogDeleteRequest');
        cy.getEntityDeleteDialogHeading('weight').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', weightPageUrl);

        weight = undefined;
      });
    });
  });

  describe('new Weight page', () => {
    beforeEach(() => {
      cy.visit(weightPageUrl);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('Weight');
    });

    it('should create an instance of Weight', () => {
      cy.get(`[data-cy="timestamp"]`).type('2023-12-08T09:19');
      cy.get(`[data-cy="timestamp"]`).blur();
      cy.get(`[data-cy="timestamp"]`).should('have.value', '2023-12-08T09:19');

      cy.get(`[data-cy="weight"]`).type('25142.93');
      cy.get(`[data-cy="weight"]`).should('have.value', '25142.93');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        weight = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.location('pathname').should('eq', weightPageUrl);
    });
  });
});
