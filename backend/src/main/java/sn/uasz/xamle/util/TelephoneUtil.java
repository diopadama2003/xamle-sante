package sn.uasz.xamle.util;

public final class TelephoneUtil {

    private TelephoneUtil() {
    }

    /** Retourne le numéro au format international, ou null s'il est invalide. */
    public static String normaliser(String brut) {
        if (brut == null) {
            return null;
        }
        String n = brut.replaceAll("[\\s.\\-()]", "");
        if (n.startsWith("00")) {
            n = "+" + n.substring(2);
        } else if (n.matches("^7\\d{8}$")) {
            n = "+221" + n;
        } else if (n.matches("^221\\d{9}$")) {
            n = "+" + n;
        }
        return n.matches("^\\+\\d{8,15}$") ? n : null;
    }
}