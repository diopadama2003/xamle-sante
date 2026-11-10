package sn.uasz.xamle.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelephoneUtilTest {

    @Test
    void numeroLocalSenegalais_recoitLIndicatif() {
        assertThat(TelephoneUtil.normaliser("771234567")).isEqualTo("+221771234567");
    }

    @Test
    void separateursSontSupprimes() {
        assertThat(TelephoneUtil.normaliser("77 123-45.67")).isEqualTo("+221771234567");
        assertThat(TelephoneUtil.normaliser("(77) 123 45 67")).isEqualTo("+221771234567");
    }

    @Test
    void prefixe00_devientPlus() {
        assertThat(TelephoneUtil.normaliser("00221771234567")).isEqualTo("+221771234567");
    }

    @Test
    void indicatifSansPlus_recoitLePlus() {
        assertThat(TelephoneUtil.normaliser("221771234567")).isEqualTo("+221771234567");
    }

    @Test
    void formatInternationalDejaCorrect_resteInchange() {
        assertThat(TelephoneUtil.normaliser("+33612345678")).isEqualTo("+33612345678");
    }

    @Test
    void numeroInvalide_retourneNull() {
        assertThat(TelephoneUtil.normaliser("abc")).isNull();
        assertThat(TelephoneUtil.normaliser("12345")).isNull();
        assertThat(TelephoneUtil.normaliser("")).isNull();
    }

    @Test
    void nul_retourneNull() {
        assertThat(TelephoneUtil.normaliser(null)).isNull();
    }
}
