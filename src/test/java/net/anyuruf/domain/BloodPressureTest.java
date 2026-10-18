package net.anyuruf.domain;

import static net.anyuruf.domain.BloodPressureTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import net.anyuruf.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BloodPressureTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(BloodPressure.class);
        BloodPressure bloodPressure1 = getBloodPressureSample1();
        BloodPressure bloodPressure2 = new BloodPressure();
        assertThat(bloodPressure1).isNotEqualTo(bloodPressure2);

        bloodPressure2.setId(bloodPressure1.getId());
        assertThat(bloodPressure1).isEqualTo(bloodPressure2);

        bloodPressure2 = getBloodPressureSample2();
        assertThat(bloodPressure1).isNotEqualTo(bloodPressure2);
    }
}
