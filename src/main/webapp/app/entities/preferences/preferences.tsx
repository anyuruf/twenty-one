import React, { useEffect, useState } from 'react';
import { Button, Col, Form, FormControl, InputGroup, Row, Table } from 'react-bootstrap';
import { Translate, getSortState, translate } from 'react-jhipster';
import { Link, useLocation, useNavigate } from 'react-router';

import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { overrideSortStateWithQueryParams } from 'app/shared/util/entity-utils';
import { ASC, DESC } from 'app/shared/util/pagination.constants';

import { getEntities, searchEntities } from './preferences.reducer';

export const Preferences = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [search, setSearch] = useState('');
  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const preferencesList = useAppSelector(state => state.preferences.entities);
  const loading = useAppSelector(state => state.preferences.loading);

  const getAllEntities = () => {
    if (search) {
      dispatch(
        searchEntities({
          query: search,
          sort: `${sortState.sort},${sortState.order}`,
        }),
      );
    } else {
      dispatch(
        getEntities({
          sort: `${sortState.sort},${sortState.order}`,
        }),
      );
    }
  };

  const startSearching = e => {
    if (search) {
      dispatch(
        searchEntities({
          query: search,
          sort: `${sortState.sort},${sortState.order}`,
        }),
      );
    }
    e.preventDefault();
  };

  const clear = () => {
    setSearch('');
    dispatch(getEntities({}));
  };

  const handleSearch = event => setSearch(event.target.value);

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?sort=${sortState.sort},${sortState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [sortState.order, sortState.sort, search]);

  const sort = p => () => {
    setSortState({
      ...sortState,
      order: sortState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = sortState.sort;
    const { order } = sortState;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="preferences-heading" data-cy="PreferencesHeading">
        <Translate contentKey="healthPointsApp.preferences.home.title">Preferences</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" variant="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="healthPointsApp.preferences.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/preferences/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="healthPointsApp.preferences.home.createLabel">Create new Preferences</Translate>
          </Link>
        </div>
      </h2>
      <Row>
        <Col sm="12">
          <Form onSubmit={startSearching}>
            <InputGroup>
              <FormControl
                type="text"
                name="search"
                defaultValue={search}
                onChange={handleSearch}
                placeholder={translate('healthPointsApp.preferences.home.search')}
              />
              <Button className="input-group-addon">
                <FontAwesomeIcon icon="search" />
              </Button>
              <Button type="reset" className="input-group-addon" onClick={clear}>
                <FontAwesomeIcon icon="trash" />
              </Button>
            </InputGroup>
          </Form>
        </Col>
      </Row>
      <div className="table-responsive">
        {preferencesList?.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="healthPointsApp.preferences.id">ID</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('weeklyGoal')}>
                  <Translate contentKey="healthPointsApp.preferences.weeklyGoal">Weekly Goal</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('weeklyGoal')} />
                </th>
                <th className="hand" onClick={sort('weightUnits')}>
                  <Translate contentKey="healthPointsApp.preferences.weightUnits">Weight Units</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('weightUnits')} />
                </th>
                <th>
                  <Translate contentKey="healthPointsApp.preferences.user">User</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {preferencesList.map(preferences => (
                <tr key={`entity-${preferences.id}`} data-cy="entityTable">
                  <td>
                    <Button as={Link as any} to={`/preferences/${preferences.id}`} variant="link" size="sm">
                      {preferences.id}
                    </Button>
                  </td>
                  <td>{preferences.weeklyGoal}</td>
                  <td>
                    <Translate contentKey={`healthPointsApp.Units.${preferences.weightUnits}`} />
                  </td>
                  <td>{preferences.user ? preferences.user.login : ''}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button as={Link as any} to={`/preferences/${preferences.id}`} variant="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button
                        as={Link as any}
                        to={`/preferences/${preferences.id}/edit`}
                        variant="primary"
                        size="sm"
                        data-cy="entityEditButton"
                      >
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (globalThis.location.href = `/preferences/${preferences.id}/delete`)}
                        variant="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                      >
                        <FontAwesomeIcon icon="trash" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.delete">Delete</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="healthPointsApp.preferences.home.notFound">No Preferences found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default Preferences;
