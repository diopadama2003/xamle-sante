package sn.uasz.xamle.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceUtilTest {

    @Test
    void memePoint_distanceNulle() {
        assertThat(DistanceUtil.km(12.5833, -16.2719, 12.5833, -16.2719)).isZero();
    }

    @Test
    void undegreDeLongitudeALEquateur_environ111km() {
        assertThat(DistanceUtil.km(0, 0, 0, 1)).isCloseTo(111.19, within(0.1));
    }

    @Test
    void distanceSymetrique() {
        double aller = DistanceUtil.km(12.5833, -16.2719, 14.7167, -17.4677);
        double retour = DistanceUtil.km(14.7167, -17.4677, 12.5833, -16.2719);
        assertThat(aller).isCloseTo(retour, within(0.0001));
    }
}
