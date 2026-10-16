package sn.uasz.xamle.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SmsService {

    @Value("${app.sms.provider:log}")
    private String provider;

    @Value("${app.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${app.sms.twilio.from:}")
    private String from;

    /** Retourne true si le SMS est parti (ou simulé), false en cas d'échec. */
    public boolean envoyer(String numero, String texte) {
        if (!"twilio".equalsIgnoreCase(provider)) {
            log.info("[SMS SIMULÉ] à {} : {}", numero, texte);
            return true;
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", numero);
            form.add("From", from);
            form.add("Body", texte);

            RestClient.create().post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json")
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            log.info("SMS envoyé via Twilio");
            return true;
        } catch (Exception e) {
            log.error("Échec de l'envoi du SMS : {}", e.getMessage());
            return false;
        }
    }
}