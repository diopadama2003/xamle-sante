package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.SpecialisteResponse;
import sn.uasz.xamle.model.Specialiste;
import sn.uasz.xamle.model.SpecialistePhoto;
import sn.uasz.xamle.model.StatutValidation;
import sn.uasz.xamle.repository.SpecialistePhotoRepository;
import sn.uasz.xamle.repository.SpecialisteRepository;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final long TAILLE_MAX = 2L * 1024 * 1024;

    private final SpecialisteRepository specialisteRepo;
    private final SpecialistePhotoRepository photoRepo;

    @Transactional
    public SpecialisteResponse enregistrer(String email, MultipartFile fichier) {
        Specialiste s = profilDe(email);

        if (fichier == null || fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier reçu");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                    "Photo trop lourde (2 Mo maximum)");
        }

        byte[] octets;
        try {
            octets = fichier.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier illisible");
        }

        // On vérifie le contenu réel du fichier, pas son nom ni le type annoncé
        String type = detecterType(octets);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formats acceptés : JPEG, PNG ou WebP");
        }

        photoRepo.save(new SpecialistePhoto(s.getId(), type, octets));
        s.setPhotoUrl("/api/specialistes/" + s.getId() + "/photo?v=" + System.currentTimeMillis());
        // Comme toute modification du profil, une nouvelle photo doit être revue par un modérateur
        s.setStatut(StatutValidation.EN_ATTENTE);
        return SpecialisteResponse.from(specialisteRepo.save(s), null);
    }

    @Transactional
    public SpecialisteResponse supprimer(String email) {
        Specialiste s = profilDe(email);
        photoRepo.deleteById(s.getId());
        s.setPhotoUrl(null);
        return SpecialisteResponse.from(specialisteRepo.save(s), null);
    }

    @Transactional(readOnly = true)
    public SpecialistePhoto photoDe(Long specialisteId) {
        return photoRepo.findById(specialisteId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucune photo"));
    }

    private Specialiste profilDe(String email) {
        return specialisteRepo.findByUtilisateurEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Enregistre d'abord ton profil"));
    }

    private String detecterType(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}