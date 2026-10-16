package sn.uasz.xamle.dto;

import java.util.List;

public record SosResponse(
        boolean smsEnvoye,
        String message,
        List<EtablissementResponse> urgences
) {}