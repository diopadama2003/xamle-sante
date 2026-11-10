package sn.uasz.xamle.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Utilisateur;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "cle-de-test-xamle-sante-0123456789-abcdef".getBytes(StandardCharsets.UTF_8));

    private JwtService service;
    private Utilisateur utilisateur;

    @BeforeEach
    void preparer() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "expirationMs", 60_000L);
        utilisateur = Utilisateur.builder().email("pro@test.sn").role(Role.PROFESSIONNEL).build();
    }

    @Test
    void tokenGenere_estValideEtContientLEmail() {
        String token = service.generateToken(utilisateur);

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractEmail(token)).isEqualTo("pro@test.sn");
    }

    @Test
    void tokenExpire_estInvalide() {
        ReflectionTestUtils.setField(service, "expirationMs", -1_000L);

        assertThat(service.isValid(service.generateToken(utilisateur))).isFalse();
    }

    @Test
    void tokenSigneAvecUneAutreCle_estInvalide() {
        String token = service.generateToken(utilisateur);
        JwtService autre = new JwtService();
        ReflectionTestUtils.setField(autre, "secret", Base64.getEncoder().encodeToString(
                "une-toute-autre-cle-secrete-0123456789-zz".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(autre, "expirationMs", 60_000L);

        assertThat(autre.isValid(token)).isFalse();
    }

    @Test
    void tokenAltere_estInvalide() {
        String token = service.generateToken(utilisateur);

        assertThat(service.isValid(token.substring(0, token.length() - 3) + "abc")).isFalse();
    }

    @Test
    void chaineQuelconqueOuVide_estInvalide() {
        assertThat(service.isValid("n-importe-quoi")).isFalse();
        assertThat(service.isValid("")).isFalse();
        assertThat(service.isValid(null)).isFalse();
    }
}
