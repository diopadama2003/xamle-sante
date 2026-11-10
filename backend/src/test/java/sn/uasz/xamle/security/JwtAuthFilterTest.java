package sn.uasz.xamle.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    JwtService jwtService;
    @Mock
    CustomUserDetailsService userDetailsService;
    @Mock
    FilterChain chaine;
    @InjectMocks
    JwtAuthFilter filtre;

    private final MockHttpServletRequest requete = new MockHttpServletRequest();
    private final MockHttpServletResponse reponse = new MockHttpServletResponse();

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sansEnTeteAuthorization_laRequetePasseNonAuthentifiee() throws Exception {
        filtre.doFilter(requete, reponse, chaine);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chaine).doFilter(requete, reponse);
        verifyNoInteractions(jwtService);
    }

    @Test
    void enTeteNonBearer_estIgnore() throws Exception {
        requete.addHeader("Authorization", "Basic abc");

        filtre.doFilter(requete, reponse, chaine);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chaine).doFilter(requete, reponse);
    }

    @Test
    void tokenValide_authentifieLUtilisateur() throws Exception {
        requete.addHeader("Authorization", "Bearer bon-token");
        when(jwtService.isValid("bon-token")).thenReturn(true);
        when(jwtService.extractEmail("bon-token")).thenReturn("pro@test.sn");
        when(userDetailsService.loadUserByUsername("pro@test.sn")).thenReturn(
                new User("pro@test.sn", "hash", List.of(new SimpleGrantedAuthority("ROLE_PROFESSIONNEL"))));

        filtre.doFilter(requete, reponse, chaine);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("pro@test.sn");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_PROFESSIONNEL");
        verify(chaine).doFilter(requete, reponse);
    }

    @Test
    void tokenInvalide_laRequetePasseNonAuthentifiee() throws Exception {
        requete.addHeader("Authorization", "Bearer mauvais");
        when(jwtService.isValid("mauvais")).thenReturn(false);

        filtre.doFilter(requete, reponse, chaine);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chaine).doFilter(requete, reponse);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void utilisateurSupprime_laRequetePasseNonAuthentifiee() throws Exception {
        requete.addHeader("Authorization", "Bearer bon-token");
        when(jwtService.isValid("bon-token")).thenReturn(true);
        when(jwtService.extractEmail("bon-token")).thenReturn("parti@test.sn");
        when(userDetailsService.loadUserByUsername("parti@test.sn"))
                .thenThrow(new UsernameNotFoundException("introuvable"));

        filtre.doFilter(requete, reponse, chaine);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chaine).doFilter(requete, reponse);
    }
}
