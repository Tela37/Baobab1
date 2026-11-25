package td.teladoum.baobab.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utilitaire de formatage et de comparaison d'horodatages.
 * Permet de vérifier si une date se situe dans une fenêtre temporelle (ex: stories éphémères de 24h).
 */
public class TimeUtils {

    public static boolean isWithinHours(String dateTimeStr, long hoursLimit) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return true;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            if (date == null) return true;

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long limitMillis = hoursLimit * 3600 * 1000L;

            // Allow up to 10 minutes in the future for clock skew
            return diffMillis >= -600000 && diffMillis <= limitMillis;
        } catch (Exception e) {
            return true;
        }
    }

    public static String getCurrentLocalDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getDefault());
        return sdf.format(new Date());
    }

    public static String getRemainingTimeText(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return "";
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            if (date == null) return "";

            long diffMillis = System.currentTimeMillis() - date.getTime();
            long total24hMillis = 24 * 3600 * 1000L;
            long remainingMillis = total24hMillis - diffMillis;

            if (remainingMillis <= 0) {
                return "Expirée";
            }

            long hours = remainingMillis / (3600 * 1000L);
            long minutes = (remainingMillis % (3600 * 1000L)) / (60 * 1000L);

            if (hours > 0) {
                return hours + "h restantes";
            } else {
                return minutes + "m restantes";
            }
        } catch (Exception e) {
            return "";
        }
    }
}

