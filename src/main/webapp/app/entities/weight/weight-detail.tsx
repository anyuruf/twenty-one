import React, { useEffect } from 'react';
import { Button, Col, Row } from 'react-bootstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { Link, useParams } from 'react-router';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './weight.reducer';

export const WeightDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id!));
  }, []);

  const weightEntity = useAppSelector(state => state.weight.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="weightDetailsHeading">
          <Translate contentKey="healthPointsApp.weight.detail.title">Weight</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{weightEntity.id}</dd>
          <dt>
            <span id="timestamp">
              <Translate contentKey="healthPointsApp.weight.timestamp">Timestamp</Translate>
            </span>
          </dt>
          <dd>{weightEntity.timestamp ? <TextFormat value={weightEntity.timestamp} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <span id="weight">
              <Translate contentKey="healthPointsApp.weight.weight">Weight</Translate>
            </span>
          </dt>
          <dd>{weightEntity.weight}</dd>
          <dt>
            <Translate contentKey="healthPointsApp.weight.user">User</Translate>
          </dt>
          <dd>{weightEntity.user ? weightEntity.user.login : ''}</dd>
        </dl>
        <Button as={Link as any} to="/weight" replace variant="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button as={Link as any} to={`/weight/${weightEntity.id}/edit`} replace variant="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default WeightDetail;
