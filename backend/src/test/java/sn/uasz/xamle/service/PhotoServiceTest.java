package sn.uasz.xamle.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.SpecialistePhoto;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.repository.SpecialistePhotoRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final String EMAIL = "pro@test.sn";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Mock
    SpecialisteRepository specialisteRepo;
    @Mock
    SpecialistePhotoRepository photoRepo;
    @InjectMocks
    PhotoService service;

    private Specialiste profil() {
        return Specialiste.builder()
                .id(1L).nom("Diop").prenom("Awa").specialite("Pédiatre")
                .statut(StatutValidation.VALIDE).build();
    }

    private void profilExiste() {
        when(specialisteRepo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.of(profil()));
    }

    private int codeErreur(Runnable action) {
        try {
            action.run();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
        return -1;
    }

    private String typeEnregistre(byte[] octets) {
        profilExiste();
        when(specialisteRepo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));
        service.enregistrer(EMAIL, new MockMultipartFile("fichier", "p", "image/x", octets));
        ArgumentCaptor<SpecialistePhoto> capture = ArgumentCaptor.forClass(SpecialistePhoto.class);
        verify(photoRepo).save(capture.capture());
        return capture.getValue().getTypeContenu();
    }

    @Test
    void enregistrer_jpeg_sauvegardeLaPhotoEtRemetLeProfilEnAttente() {
        profilExiste();
        when(specialisteRepo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialisteResponse reponse = service.enregistrer(EMAIL,
                new MockMultipartFile("fichier", "profil.jpg", "image/jpeg", JPEG));

        assertThat(reponse.photoUrl()).startsWith("/api/specialistes/1/photo?v=");
        assertThat(reponse.statut()).isEqualTo(StatutValidation.EN_ATTENTE);
        ArgumentCaptor<SpecialistePhoto> capture = ArgumentCaptor.forClass(SpecialistePhoto.class);
        verify(photoRepo).save(capture.capture());
        assertThat(capture.getValue().getSpecialisteId()).isEqualTo(1L);
        assertThat(capture.getValue().getTypeContenu()).isEqualTo("image/jpeg");
    }

    @Test
    void enregistrer_reconnaitLePng() {
        assertThat(typeEnregistre(PNG)).isEqualTo("image/png");
    }

    @Test
    void enregistrer_reconnaitLeWebp() {
        assertThat(typeEnregistre(WEBP)).isEqualTo("image/webp");
    }

    @Test
    void enregistrer_fichierQuiNestPasUneImage_415() {
        profilExiste();
        MockMultipartFile texte = new MockMultipartFile("fichier", "faux.jpg", "image/jpeg",
                "bonjour".getBytes());

        assertThat(codeErreur(() -> service.enregistrer(EMAIL, texte))).isEqualTo(415);
        verify(photoRepo, never()).save(any());
    }

    @Test
    void enregistrer_fichierVide_400() {
        profilExiste();
        MockMultipartFile vide = new MockMultipartFile("fichier", "p.jpg", "image/jpeg", new byte[0]);

        assertThat(codeErreur(() -> service.enregistrer(EMAIL, vide))).isEqualTo(400);
    }

    @Test
    void enregistrer_photoTropLourde_413() {
        profilExiste();
        MockMultipartFile gros = new MockMultipartFile("fichier", "p.jpg", "image/jpeg",
                new byte[2 * 1024 * 1024 + 1]);

        assertThat(codeErreur(() -> service.enregistrer(EMAIL, gros))).isEqualTo(413);
        verify(photoRepo, never()).save(any());
    }

    @Test
    void enregistrer_sansProfil_404() {
        when(specialisteRepo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.empty());
        MockMultipartFile fichier = new MockMultipartFile("fichier", "p.jpg", "image/jpeg", JPEG);

        assertThat(codeErreur(() -> service.enregistrer(EMAIL, fichier))).isEqualTo(404);
    }

    @Test
    void supprimer_efface_laPhotoEtLUrl() {
        Specialiste s = profil();
        s.setPhotoUrl("/api/specialistes/1/photo?v=1");
        when(specialisteRepo.findByUtilisateurEmail(EMAIL)).thenReturn(Optional.of(s));
        when(specialisteRepo.save(any(Specialiste.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialisteResponse reponse = service.supprimer(EMAIL);

        verify(photoRepo).deleteById(1L);
        assertThat(reponse.photoUrl()).isNull();
    }

    @Test
    void photoDe_existante_laRetourne() {
        SpecialistePhoto photo = new SpecialistePhoto(1L, "image/jpeg", JPEG);
        when(photoRepo.findById(1L)).thenReturn(Optional.of(photo));

        assertThat(service.photoDe(1L)).isSameAs(photo);
    }

    @Test
    void photoDe_inconnue_404() {
        when(photoRepo.findById(9L)).thenReturn(Optional.empty());

        assertThat(codeErreur(() -> service.photoDe(9L))).isEqualTo(404);
    }
}
