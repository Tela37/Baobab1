package td.teladoum.baobab.utils;

import android.util.Patterns;

/**
 * Utilitaire de validation de données (E-mail, Téléphone, Mot de passe, Champs de saisie).
 */
public class ValidationUtils {

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return Patterns.PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isValidPassword(String password, int minLength) {
        if (password == null) {
            return false;
        }
        return password.length() >= minLength;
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private ValidationUtils() {
    }
}
