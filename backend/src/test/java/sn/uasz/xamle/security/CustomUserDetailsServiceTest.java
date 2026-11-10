package sn.uasz.xamle.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import sn.uasz.xamle.model.Role;
import sn.uasz.xamle.model.Utilisateur;
import sn.uasz.xamle.repository.UtilisateurRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UtilisateurRepository utilisateurs;
    @InjectMocks
    CustomUserDetailsService service;

    private Utilisateur utilisateur(boolean actif) {
        return Utilisateur.builder().email("mod@test.sn").motDePasse("hash").role(Role.MODERATEUR)
                .actif(actif).build();
    }

    @Test
    void utilisateurExistant_recoitSonRoleAvecLePrefixeROLE() {
        when(utilisateurs.findByEmail("mod@test.sn")).thenReturn(Optional.of(utilisateur(true)));

        UserDetails details = service.loadUserByUsername("mod@test.sn");

        assertThat(details.getUsername()).isEqualTo("mod@test.sn");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MODERATEUR");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void compteDesactive_estMarqueInactif() {
        when(utilisateurs.findByEmail("mod@test.sn")).thenReturn(Optional.of(utilisateur(false)));

        assertThat(service.loadUserByUsername("mod@test.sn").isEnabled()).isFalse();
    }

    @Test
    void utilisateurInconnu_leveUneException() {
        when(utilisateurs.findByEmail("x@test.sn")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("x@test.sn"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
