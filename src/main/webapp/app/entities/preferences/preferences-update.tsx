import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { Translate, ValidatedField, ValidatedForm, isNumber, translate } from 'react-jhipster';
import { Link, useNavigate, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { Units } from 'app/shared/model/enumerations/units.model';
import { getUsers } from 'app/shared/reducers/user-management';

import { createEntity, getEntity, reset, updateEntity } from './preferences.reducer';

export const PreferencesUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const users = useAppSelector(state => state.userManagement.users);
  const preferencesEntity = useAppSelector(state => state.preferences.entity);
  const loading = useAppSelector(state => state.preferences.loading);
  const updating = useAppSelector(state => state.preferences.updating);
  const updateSuccess = useAppSelector(state => state.preferences.updateSuccess);
  const unitsValues = Object.keys(Units);

  const handleClose = () => {
    navigate('/preferences');
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }
    if (values.weeklyGoal !== undefined && typeof values.weeklyGoal !== 'number') {
      values.weeklyGoal = Number(values.weeklyGoal);
    }

    const entity = {
      ...preferencesEntity,
      ...values,
      user: users.find(it => it.id.toString() === values.user?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {}
      : {
          weightUnits: 'KG',
          ...preferencesEntity,
          user: preferencesEntity?.user?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="healthPointsApp.preferences.home.createOrEditLabel" data-cy="PreferencesCreateUpdateHeading">
            <Translate contentKey="healthPointsApp.preferences.home.createOrEditLabel">Create or edit a Preferences</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm mode="all" defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew && (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="preferences-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              )}
              <ValidatedField
                label={translate('healthPointsApp.preferences.weeklyGoal')}
                id="preferences-weeklyGoal"
                name="weeklyGoal"
                data-cy="weeklyGoal"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                  min: { value: 10, message: translate('entity.validation.min', { min: 10 }) },
                  max: { value: 21, message: translate('entity.validation.max', { max: 21 }) },
                  validate: v => isNumber(v) || translate('entity.validation.number'),
                }}
              />
              <ValidatedField
                label={translate('healthPointsApp.preferences.weightUnits')}
                id="preferences-weightUnits"
                name="weightUnits"
                data-cy="weightUnits"
                type="select"
              >
                {unitsValues.map(units => (
                  <option value={units} key={units}>
                    {translate(`healthPointsApp.Units.${units}`)}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                id="preferences-user"
                name="user"
                data-cy="user"
                label={translate('healthPointsApp.preferences.user')}
                type="select"
              >
                <option value="" key="0" />
                {users
                  ? users.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.login}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button as={Link as any} id="cancel-save" data-cy="entityCreateCancelButton" to="/preferences" replace variant="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button variant="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default PreferencesUpdate;
