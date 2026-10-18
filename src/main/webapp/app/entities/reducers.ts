import bloodPressure from 'app/entities/blood-pressure/blood-pressure.reducer';
import points from 'app/entities/points/points.reducer';
import preferences from 'app/entities/preferences/preferences.reducer';
import weight from 'app/entities/weight/weight.reducer';
/* jhipster-needle-add-reducer-import - JHipster will add reducer here */

const entitiesReducers = {
  points,
  bloodPressure,
  weight,
  preferences,
  // jhipster-needle-add-reducer-combine - JHipster will add reducer here
};

export default entitiesReducers;
