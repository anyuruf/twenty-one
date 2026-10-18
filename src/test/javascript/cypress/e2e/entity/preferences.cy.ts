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

describe('Preferences e2e test', () => {
  const preferencesPageUrl = '/preferences';
  let username: string;
  let password: string;
  const preferencesSample = { weeklyGoal: 18, weightUnits: 'KG' };

  let preferences;

  before(() => {
    cy.credentials().then(credentials => {
      ({ username, password } = credentials);
    });
  });

  beforeEach(() => {
    cy.login(username, password);
  });

  beforeEach(() => {
    cy.intercept('GET', '/api/preferences+(?*|)').as('entitiesRequest');
    cy.intercept('POST', '/api/preferences').as('postEntityRequest');
    cy.intercept('DELETE', '/api/preferences/*').as('deleteEntityRequest');
  });

  afterEach(() => {
    if (preferences) {
      cy.authenticatedRequest({
        method: 'DELETE',
        url: `/api/preferences/${preferences.id}`,
      }).then(() => {
        preferences = undefined;
      });
    }
  });

  it('Preferenceses menu should load Preferenceses page', () => {
    cy.visit('/');
    cy.clickOnEntityMenuItem('preferences');
    cy.wait('@entitiesRequest').then(({ response }) => {
      if (response?.body.length === 0) {
        cy.get(entityTableSelector).should('not.exist');
      } else {
        cy.get(entityTableSelector).should('exist');
      }
    });
    cy.getEntityHeading('Preferences').should('exist');
    cy.location('pathname').should('eq', preferencesPageUrl);
  });

  describe('Preferences page', () => {
    it('should have translated page title', () => {
      cy.visit(preferencesPageUrl);
      cy.getEntityHeading('Preferences').should('not.contain', 'healthPointsApp.preferences.home.title');
    });

    describe('create button click', () => {
      beforeEach(() => {
        cy.visit(preferencesPageUrl);
        cy.wait('@entitiesRequest');
      });

      it('should load create Preferences page', () => {
        cy.get(entityCreateButtonSelector).click();
        cy.location('pathname').should('eq', `${preferencesPageUrl}/new`);
        cy.getEntityCreateUpdateHeading('Preferences');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', preferencesPageUrl);
      });
    });

    describe('with existing value', () => {
      beforeEach(() => {
        cy.authenticatedRequest({
          method: 'POST',
          url: '/api/preferences',
          body: preferencesSample,
        }).then(({ body }) => {
          preferences = body;

          cy.intercept(
            {
              method: 'GET',
              url: '/api/preferences+(?*|)',
              times: 1,
            },
            {
              statusCode: 200,
              body: [preferences],
            },
          ).as('entitiesRequestInternal');
        });

        cy.visit(preferencesPageUrl);

        cy.wait('@entitiesRequestInternal');
      });

      it('detail button click should load details Preferences page', () => {
        cy.get(entityDetailsButtonSelector).first().click();
        cy.getEntityDetailsHeading('preferences');
        cy.get(entityDetailsBackButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', preferencesPageUrl);
      });

      it('edit button click should load edit Preferences page and go back', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Preferences');
        cy.get(entityCreateSaveButtonSelector).should('exist');
        cy.get(entityCreateCancelButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', preferencesPageUrl);
      });

      it('edit button click should load edit Preferences page and save', () => {
        cy.get(entityEditButtonSelector).first().click();
        cy.getEntityCreateUpdateHeading('Preferences');
        cy.get(entityCreateSaveButtonSelector).click();
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', preferencesPageUrl);
      });

      it('last delete button click should delete instance of Preferences', () => {
        cy.intercept('GET', '/api/preferences/*').as('dialogDeleteRequest');
        cy.get(entityDeleteButtonSelector).last().click();
        cy.wait('@dialogDeleteRequest');
        cy.getEntityDeleteDialogHeading('preferences').should('exist');
        cy.get(entityConfirmDeleteButtonSelector).click();
        cy.wait('@deleteEntityRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(204);
        });
        cy.wait('@entitiesRequest').then(({ response }) => {
          expect(response?.statusCode).to.equal(200);
        });
        cy.location('pathname').should('eq', preferencesPageUrl);

        preferences = undefined;
      });
    });
  });

  describe('new Preferences page', () => {
    beforeEach(() => {
      cy.visit(preferencesPageUrl);
      cy.get(entityCreateButtonSelector).click();
      cy.getEntityCreateUpdateHeading('Preferences');
    });

    it('should create an instance of Preferences', () => {
      cy.get(`[data-cy="weeklyGoal"]`).type('18');
      cy.get(`[data-cy="weeklyGoal"]`).should('have.value', '18');

      cy.get(`[data-cy="weightUnits"]`).select('KG');

      cy.get(entityCreateSaveButtonSelector).click();

      cy.wait('@postEntityRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(201);
        preferences = response.body;
      });
      cy.wait('@entitiesRequest').then(({ response }) => {
        expect(response?.statusCode).to.equal(200);
      });
      cy.location('pathname').should('eq', preferencesPageUrl);
    });
  });
});
