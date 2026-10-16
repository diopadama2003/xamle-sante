package sn.uasz.xamle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sn.uasz.xamle.dto.EtablissementResponse;
import sn.uasz.xamle.dto.SosRequest;
import sn.uasz.xamle.dto.SosResponse;
import sn.uasz.xamle.model.SosAlerte;
import sn.uasz.xamle.repository.SosAlerteRepository;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SosService {

    private static final int MAX_PAR_FENETRE = 5;
    private static final long FENETRE_MS = 10 * 60 * 1000L;

    private final EtablissementService etablissementService;
    private final SmsService smsService;
    private final SosAlerteRepository alerteRepository;

    // Limite anti-abus : l'endpoint est public, donc on borne les envois par adresse IP
    private final Map<String, Deque<Long>> historique = new ConcurrentHashMap<>();

    public SosResponse declencher(SosRequest req, String ip) {
        verifierLimite(ip);

        String numero = normaliser(req.telephone());
        List<EtablissementResponse> urgences =
                etablissementService.urgencesProches(req.latitude(), req.longitude());
        List<EtablissementResponse> proches = urgences.stream().limit(2).toList();

        String lien = String.format(Locale.US, "https://maps.google.com/?q=%.6f,%.6f",
                req.latitude(), req.longitude());

        StringBuilder sms = new StringBuilder(
                "URGENCE (Xamlé Santé) : une personne a besoin d'aide. Position : ")
                .append(lien);
        if (!proches.isEmpty()) {
            sms.append(" Urgences proches :");
            for (EtablissementResponse e : proches) {
                sms.append(' ').append(e.nom())
                        .append(String.format(Locale.US, " (%.1f km)", e.distanceKm()));
            }
        }

        boolean envoye = smsService.envoyer(numero, sms.toString());

        alerteRepository.save(SosAlerte.builder()
                .latitude(req.latitude())
                .longitude(req.longitude())
                .contactMasque(masquer(numero))
                .smsEnvoye(envoye)
                .build());

        return new SosResponse(envoye, sms.toString(), urgences);
    }

    private String normaliser(String brut) {
        String n = brut.replaceAll("[\\s.\\-()]", "");
        if (n.startsWith("00")) {
            n = "+" + n.substring(2);
        } else if (n.matches("^7\\d{8}$")) {
            n = "+221" + n;
        } else if (n.matches("^221\\d{9}$")) {
            n = "+" + n;
        }
        if (!n.matches("^\\+\\d{8,15}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numéro de téléphone invalide");
        }
        return n;
    }

    private String masquer(String numero) {
        return numero.substring(0, 4) + "*****" + numero.substring(numero.length() - 3);
    }

    private void verifierLimite(String ip) {
        long maintenant = System.currentTimeMillis();
        Deque<Long> envois = historique.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (envois) {
            while (!envois.isEmpty() && maintenant - envois.peekFirst() > FENETRE_MS) {
                envois.pollFirst();
            }
            if (envois.size() >= MAX_PAR_FENETRE) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Trop de demandes, réessaie dans quelques minutes");
            }
            envois.addLast(maintenant);
        }
    }
}