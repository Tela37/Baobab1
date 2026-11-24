package td.teladoum.baobab.helpers;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Utilitaire pour la gestion moderne et rétrocompatible des barres système (Barre de statut et Barre de navigation).
 * Conforme aux directives d'Android 15 (SDK 35+) en utilisant WindowInsetsControllerCompat.
 */
public class SystemBarHelper {

    /**
     * Applique la configuration optimale des icônes de barres système (clair/sombre)
     * selon le mode d'affichage actuel de l'activité.
     *
     * @param activity L'activité cible
     */
    public static void applySystemBarAppearance(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        try {
            boolean isDarkMode = (activity.getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());

            if (controller != null) {
                // Mode clair -> Icônes sombres (!isDarkMode = true)
                // Mode sombre -> Icônes claires (!isDarkMode = false)
                controller.setAppearanceLightStatusBars(!isDarkMode);
                controller.setAppearanceLightNavigationBars(!isDarkMode);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Applique manuellement un mode d'icônes spécfique (ex: forces icônes sombres ou claires).
     *
     * @param activity L'activité cible
     * @param isLightBars true pour des icônes sombres (fond clair), false pour des icônes claires (fond sombre)
     */
    public static void applyCustomSystemBarAppearance(Activity activity, boolean isLightBars) {
        if (activity == null || activity.getWindow() == null) return;
        try {
            WindowInsetsControllerCompat controller =
                    WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());

            if (controller != null) {
                controller.setAppearanceLightStatusBars(isLightBars);
                controller.setAppearanceLightNavigationBars(isLightBars);
            }
        } catch (Exception ignored) {}
    }
}
